package com.example.fatecCarCarona.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.dto.NearbyDriversDTO;
import com.example.fatecCarCarona.dto.RouteCoordinatesDTO;
import com.example.fatecCarCarona.entity.PassageRequestQueue;
import com.example.fatecCarCarona.entity.PassageRequestQueueStatus;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.PassageRequestsPipelineStatus;
import com.example.fatecCarCarona.repository.PassageRequestQueueRepository;
import com.example.fatecCarCarona.repository.PassageRequestQueueStatusRepository;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.PassageRequestsPipelineStatusRepository;
import com.example.fatecCarCarona.repository.RideRepository;

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.OptimisticLockException;

@Service
@Slf4j
public class PassageRequestAutomaticService {

	@Autowired
	private PassageRequestsRepository passageRequestsRepository;

	@Autowired
	private PassageRequestQueueRepository passageRequestQueueRepository;

	@Autowired
	private PassageRequestQueueStatusRepository queueStatusRepository;

	@Autowired
	private com.example.fatecCarCarona.repository.UserRepository userRepository;

	@Autowired
	private PassageRequestsPipelineStatusRepository pipelineStatusRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private FindNearbyDrivers findNearbyDrivers;

	@Autowired
	private SseNotificationService sseNotificationService;

	@Autowired
	private RideRepository rideRepository;

	@Autowired
	private PassageRequestsStatusService passageRequestsStatusService;

	@Value("${carona.auto.timeout-motorista-segundos:60}")
	private Integer timeoutMotoristaSegundos;

	@Value("${carona.auto.limite-tentativas:3}")
	private Integer limiteTentativas;

	// ScheduledExecutorService para gerenciar timeouts
	private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);

	// map to track scheduled timeouts by filaId so we can cancel them when needed
	private final ConcurrentMap<Long, ScheduledFuture<?>> scheduledTimeouts = new ConcurrentHashMap<>();

	/**
	 * Inicia o fluxo automático de carona
	 *
	 * @param passageRequestId ID da solicitação de carona
	 * @param latitudeOrigem Latitude de origem
	 * @param longitudeOrigem Longitude de origem
	 * @param latitudeDestino Latitude de destino
	 * @param longitudeDestino Longitude de destino
	 */
	@Transactional
	public void iniciarFluxoAutomatico(Long passageRequestId, Double latitudeOrigem, Double longitudeOrigem,
			Double latitudeDestino, Double longitudeDestino) throws Exception {

		log.info("Iniciando fluxo automático para solicitação: {}", passageRequestId);

		// Buscar solicitação
		PassageRequests solicitacao = passageRequestsRepository.findById(passageRequestId)
				.orElseThrow(() -> new Exception("Solicitação não encontrada"));

		// If frontend does not send coordinates, use the persisted coordinates from the solicitation.
		if (latitudeOrigem == null || longitudeOrigem == null || latitudeDestino == null || longitudeDestino == null) {
			latitudeOrigem = solicitacao.getOrigin().getLatitude();
			longitudeOrigem = solicitacao.getOrigin().getLongitude();
			latitudeDestino = solicitacao.getDestination().getLatitude();
			longitudeDestino = solicitacao.getDestination().getLongitude();
		}

		// Buscar motoristas próximos
		List<NearbyDriversDTO> motoristasProximos = findNearbyDrivers.NearbyDriversService(
				new RouteCoordinatesDTO(latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino));

		if (motoristasProximos == null || motoristasProximos.isEmpty()) {
			log.warn("Nenhum motorista próximo encontrado para solicitação: {}", passageRequestId);
			marcarSolicitacaoComoRecusada(solicitacao);
			atualizarStatusPipeline(solicitacao, "falha_final");
			notificarPassageiro(solicitacao.getPassageiro().getId(), "nenhum_motorista",
					"Nenhum motorista disponível no momento. Tente novamente mais tarde.");
			return;
		}

		// Configurar status pipeline como "aguardando"
		PassageRequestsPipelineStatus statusAguardando = pipelineStatusRepository.findByNome("aguardando")
				.orElseThrow(() -> new Exception("Status 'aguardando' não encontrado"));
		solicitacao.setStatusPipeline(statusAguardando);

		// Inicializar tentativa
		solicitacao.setTentativaAtual(0);
		passageRequestsRepository.save(solicitacao);

		// Criar fila de motoristas
		criarFilaMotoristas(solicitacao, motoristasProximos);

		// Enviar para primeiro motorista
		enviarProximoMotorista(solicitacao);
	}

	/**
	 * Cria a fila de motoristas ordenados por proximidade
	 */
	@Transactional
	public void criarFilaMotoristas(PassageRequests solicitacao, List<NearbyDriversDTO> motoristas) throws Exception {

		PassageRequestQueueStatus statusPendente = queueStatusRepository.findByNome("pendente")
				.orElseThrow(() -> new Exception("Status 'pendente' não encontrado"));

		// Limpar fila anterior se existir
		passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(solicitacao.getId())
				.forEach(passageRequestQueueRepository::delete);

		int ordem = 1;
		for (NearbyDriversDTO motorista : motoristas) {
			PassageRequestQueue filaEntry = new PassageRequestQueue();
			filaEntry.setSolicitacao(solicitacao);
			// Defensive: skip invalid candidates instead of failing the whole flow
			var motoristaUserOpt = userRepository.findById(motorista.idMotorista());
			if (motoristaUserOpt.isEmpty()) {
				log.warn("Motorista candidato não encontrado: {} - pulando", motorista.idMotorista());
				continue;
			}
			var motoristaUser = motoristaUserOpt.get();
			if (motorista.idCarona() == null) {
				log.warn("Motorista candidato sem idCarona: {} - pulando", motorista.idMotorista());
				continue;
			}
			var rideOpt = rideRepository.findById(motorista.idCarona());
			if (rideOpt.isEmpty()) {
				log.warn("Carona candidata não encontrada: {} - pulando", motorista.idCarona());
				continue;
			}
			var ride = rideOpt.get();
			filaEntry.setMotorista(motoristaUser);
			filaEntry.setRide(ride);
			filaEntry.setOrdemFila(ordem);
			filaEntry.setStatus(statusPendente);
			filaEntry.setDistanciaOrigemKm(motorista.distanciaOrigemKm());

			passageRequestQueueRepository.save(filaEntry);
			ordem++;

			log.debug("Motorista {} adicionado à fila na posição {}", motorista.idMotorista(), ordem - 1);
		}
	}

	/**
	 * Envia a solicitação para o próximo motorista na fila
	 */
	@Transactional
	public void enviarProximoMotorista(PassageRequests solicitacao) throws Exception {

		log.info("Enviando para próximo motorista. Tentativa atual: {}", solicitacao.getTentativaAtual());

		// Verificar limite de tentativas
		// Refresh solicitation from DB to avoid stale state
		solicitacao = passageRequestsRepository.findById(solicitacao.getId())
				.orElseThrow(() -> new Exception("Solicitação não encontrada"));

		// Verify pipeline is still awaiting
		if (solicitacao.getStatusPipeline() != null && !solicitacao.getStatusPipeline().getNome().equals("aguardando")) {
			log.info("Pipeline não está mais aguardando para solicitação {} - abortando envio", solicitacao.getId());
			return;
		}

		if (solicitacao.getTentativaAtual() >= limiteTentativas) {
			log.warn("Limite de tentativas atingido para solicitação: {}", solicitacao.getId());
			marcarSolicitacaoComoRecusada(solicitacao);
			atualizarStatusPipeline(solicitacao, "falha_final");
			notificarPassageiro(solicitacao.getPassageiro().getId(), "falha_final",
					"Nenhum motorista aceitou sua solicitação. Solicite novamente.");
			return;
		}

		// Buscar próximo motorista com status "pendente"
		PassageRequestQueue proximoNaFila = passageRequestQueueRepository
				.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(solicitacao.getId(), "pendente")
					.orElse(null);

		if (proximoNaFila == null) {
			log.warn("Nenhum motorista pendente na fila para solicitação: {}", solicitacao.getId());
			marcarSolicitacaoComoRecusada(solicitacao);
			atualizarStatusPipeline(solicitacao, "falha_final");
			notificarPassageiro(solicitacao.getPassageiro().getId(), "falha_final",
					"Nenhum motorista disponível. Tente novamente.");
			return;
		}

		// Atualizar status para "enviada"
		PassageRequestQueueStatus statusEnviada = queueStatusRepository.findByNome("enviada")
				.orElseThrow(() -> new Exception("Status 'enviada' não encontrado"));

		proximoNaFila.setStatus(statusEnviada);
		proximoNaFila.setDataEnvio(LocalDateTime.now());
		passageRequestQueueRepository.save(proximoNaFila);

		// Incrementar a tentativa somente quando um motorista foi efetivamente acionado
		solicitacao.setTentativaAtual(solicitacao.getTentativaAtual() + 1);
		passageRequestsRepository.save(solicitacao);

		// Enviar notificação SSE para o motorista
		notificarMotorista(proximoNaFila.getMotorista().getId(), "nova_solicitacao",
				construirDadosNotificacao(solicitacao, proximoNaFila));

		// Configurar timeout para esta tentativa e track the future
		agendarTimeoutMotorista(proximoNaFila.getId(), solicitacao.getId());

		log.info("Notificação enviada para motorista {} na tentativa {}", proximoNaFila.getMotorista().getId(),
				solicitacao.getTentativaAtual());
	}

	/**
	 * Processa a aceitação de um motorista
	 */
	@Transactional
	public void handleMotoristaAceita(Long filaId, Long solicitacaoId, Long motoristaId) throws Exception {

		log.info("Motorista {} aceitou solicitação {}", motoristaId, solicitacaoId);

		PassageRequestQueue fila = passageRequestQueueRepository.findById(filaId)
				.orElseThrow(() -> new Exception("Entrada de fila não encontrada"));

		PassageRequests solicitacao = passageRequestsRepository.findById(solicitacaoId)
				.orElseThrow(() -> new Exception("Solicitação não encontrada"));

		// Validate that fila was actually sent and solicitation is still awaiting
		if (!fila.getStatus().getNome().equals("enviada")) {
			log.warn("Fila {} não está em 'enviada' (status={}) - aceitação inválida", filaId, fila.getStatus().getNome());
			throw new IllegalStateException("Aceitação inválida: fila não está em 'enviada'");
		}
		if (solicitacao.getStatusPipeline() != null && !solicitacao.getStatusPipeline().getNome().equals("aguardando")) {
			log.warn("Solicitação {} pipeline está em {} - aceitação inválida", solicitacaoId,
					solicitacao.getStatusPipeline().getNome());
			throw new IllegalStateException("Aceitação inválida: solicitação não está aguardando");
		}

		// Atualizar status da entrada de fila para "aceita"
		PassageRequestQueueStatus statusAceita = queueStatusRepository.findByNome("aceita")
				.orElseThrow(() -> new Exception("Status 'aceita' não encontrado"));
		fila.setStatus(statusAceita);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

		// cancel scheduled timeout for this fila
		cancelScheduledTimeout(filaId);

		// Atualizar pipeline da solicitação para 'aceita'
		atualizarStatusPipeline(solicitacao, "aceita");

		// Atualizar status da solicitação como aceita
		solicitacao.setCarona(fila.getRide());
		solicitacao.setStatus(passageRequestsStatusService.findByNome("aceita"));
		try {
			passageRequestsRepository.save(solicitacao);
		} catch (OptimisticLockException ole) {
			log.warn("Conflito otimista ao salvar solicitação {}: {}", solicitacaoId, ole.getMessage());
			throw new IllegalStateException("Solicitação já processada por outro motorista");
		}

		// Notificar passageiro sobre aceitação
		notificarPassageiro(solicitacao.getPassageiro().getId(), "solicitacao_aceita",
				"Seu motorista aceitou a solicitação!");

		// Rejeitar outros motoristas na fila
		rejeitarOutrosMotoristas(solicitacao.getId(), motoristaId);

		log.info("Solicitação {} aceita pelo motorista {}", solicitacaoId, motoristaId);
	}

	/**
	 * Processa a recusa de um motorista
	 */
	@Transactional
	public void handleMotoristaRecusa(Long filaId, Long solicitacaoId) throws Exception {

		log.info("Motorista recusou solicitação {}", solicitacaoId);

		PassageRequestQueue fila = passageRequestQueueRepository.findById(filaId)
				.orElseThrow(() -> new Exception("Entrada de fila não encontrada"));

		PassageRequests solicitacao = passageRequestsRepository.findById(solicitacaoId)
				.orElseThrow(() -> new Exception("Solicitação não encontrada"));

		// Ensure fila was actually sent
		if (!fila.getStatus().getNome().equals("enviada")) {
			log.warn("Recusa inválida: fila {} não está em 'enviada' (status={})", filaId, fila.getStatus().getNome());
			return;
		}

		// Atualizar status para "recusada"
		PassageRequestQueueStatus statusRecusada = queueStatusRepository.findByNome("recusada")
				.orElseThrow(() -> new Exception("Status 'recusada' não encontrado"));
		fila.setStatus(statusRecusada);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

		// cancel scheduled timeout for this fila (explicit recusa)
		cancelScheduledTimeout(filaId);

		// Enviar para próximo motorista
		enviarProximoMotorista(solicitacao);
	}

	/**
	 * Processa timeout de um motorista (não respondeu)
	 */
	@Transactional
	public void handleTimeoutMotorista(Long filaId, Long solicitacaoId) throws Exception {

		log.warn("Timeout para motorista na fila {}, solicitação {}", filaId, solicitacaoId);

		PassageRequestQueue fila = passageRequestQueueRepository.findById(filaId)
				.orElseThrow(() -> new Exception("Entrada de fila não encontrada"));

		PassageRequests solicitacao = passageRequestsRepository.findById(solicitacaoId)
				.orElseThrow(() -> new Exception("Solicitação não encontrada"));

		// Ensure fila was actually sent (avoid race)
		if (!fila.getStatus().getNome().equals("enviada")) {
			log.info("Timeout ignored: fila {} status is {}", filaId, fila.getStatus().getNome());
			return;
		}

		// Atualizar status para "timeout"
		PassageRequestQueueStatus statusTimeout = queueStatusRepository.findByNome("timeout")
				.orElseThrow(() -> new Exception("Status 'timeout' não encontrado"));
		fila.setStatus(statusTimeout);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

		// remove scheduled future entry if present
		scheduledTimeouts.remove(filaId);

		// Enviar para próximo motorista
		enviarProximoMotorista(solicitacao);
	}

	/**
	 * Rejeita todos os outros motoristas na fila quando um aceita
	 */
	@Transactional
	public void rejeitarOutrosMotoristas(Long solicitacaoId, Long motoristaAceitouId) throws Exception {

		PassageRequestQueueStatus statusRecusada = queueStatusRepository.findByNome("recusada")
				.orElseThrow(() -> new Exception("Status 'recusada' não encontrado"));

		List<PassageRequestQueue> filasAntigos = passageRequestQueueRepository
				.findBySolicitacaoIdOrderByOrdemFilaAsc(solicitacaoId);

		for (PassageRequestQueue fila : filasAntigos) {
			if (!fila.getMotorista().getId().equals(motoristaAceitouId)
					&& !fila.getStatus().getNome().equals("recusada")
					&& !fila.getStatus().getNome().equals("aceita")
					&& !fila.getStatus().getNome().equals("timeout")) {
				fila.setStatus(statusRecusada);
				fila.setDataResposta(LocalDateTime.now());
				passageRequestQueueRepository.save(fila);

				log.debug("Motorista {} rejeitado automaticamente", fila.getMotorista().getId());
			}
		}
	}

	/**
	 * Agenda timeout para um motorista
	 */
	private void agendarTimeoutMotorista(Long filaId, Long solicitacaoId) {

		ScheduledFuture<?> future = timeoutExecutor.schedule(() -> {
			try {
				// Verificar se ainda está com status "enviada"
				PassageRequestQueue fila = passageRequestQueueRepository.findById(filaId).orElse(null);

				if (fila != null && fila.getStatus().getNome().equals("enviada")) {
					// Invoke via proxy so @Transactional on handleTimeoutMotorista is applied
					try {
						PassageRequestAutomaticService proxy = applicationContext.getBean(PassageRequestAutomaticService.class);
						proxy.handleTimeoutMotorista(filaId, solicitacaoId);
					} catch (Exception e) {
						log.error("Erro ao invocar handleTimeoutMotorista via proxy: {}", e.getMessage());
					}
				}
			} catch (Exception e) {
				log.error("Erro ao processar timeout de motorista: {}", e.getMessage());
			} finally {
				// ensure we remove tracking after run
				scheduledTimeouts.remove(filaId);
			}
		}, timeoutMotoristaSegundos, TimeUnit.SECONDS);

		// track future so it can be cancelled if the driver accepts or flow finalizes
		scheduledTimeouts.put(filaId, future);
	}

	private void cancelScheduledTimeout(Long filaId) {
		ScheduledFuture<?> future = scheduledTimeouts.remove(filaId);
		if (future != null) {
			future.cancel(false);
			log.debug("Cancelled scheduled timeout for fila {}", filaId);
		}
	}

	/**
	 * Atualiza o status do pipeline da solicitação
	 */
	@Transactional
	public void atualizarStatusPipeline(PassageRequests solicitacao, String novoStatus) throws Exception {

		PassageRequestsPipelineStatus status = pipelineStatusRepository.findByNome(novoStatus)
				.orElseThrow(() -> new Exception("Status pipeline '" + novoStatus + "' não encontrado"));

		solicitacao.setStatusPipeline(status);
		passageRequestsRepository.save(solicitacao);

		log.debug("Status pipeline atualizado para: {}", novoStatus);
	}

	@Transactional
	private void marcarSolicitacaoComoRecusada(PassageRequests solicitacao) throws Exception {
		var statusRecusada = passageRequestsStatusService.findByNome("recusada");
		if (statusRecusada == null) {
			throw new Exception("Status 'recusada' não encontrado");
		}
		solicitacao.setStatus(statusRecusada);
		passageRequestsRepository.save(solicitacao); // ✅ SALVAR NO BD

		// Finalizar todas as filas associadas que ainda não têm status final
		PassageRequestQueueStatus statusRecusadaFila = queueStatusRepository.findByNome("recusada")
				.orElseThrow(() -> new Exception("Status 'recusada' não encontrado"));

		List<PassageRequestQueue> filas = passageRequestQueueRepository
				.findBySolicitacaoIdOrderByOrdemFilaAsc(solicitacao.getId());

		for (PassageRequestQueue fila : filas) {
			String nomeStatus = fila.getStatus() != null ? fila.getStatus().getNome() : "";
			if (!nomeStatus.equals("recusada") && !nomeStatus.equals("aceita") && !nomeStatus.equals("timeout")) {
				fila.setStatus(statusRecusadaFila);
				fila.setDataResposta(LocalDateTime.now());
				passageRequestQueueRepository.save(fila);

				// cancel any scheduled timeout for this fila
				cancelScheduledTimeout(fila.getId());
			}
		}

		log.info("Solicitação {} marcada como recusada e filas finalizadas", solicitacao.getId());
	}

	/**
	 * Constrói dados para notificação de nova solicitação
	 */
	private Object construirDadosNotificacao(PassageRequests solicitacao, PassageRequestQueue fila) {
		return Map.of("solicitacaoId", solicitacao.getId(), "filaId", fila.getId(),
				"passageiroId", solicitacao.getPassageiro().getId(),
				"passageiroNome", solicitacao.getPassageiro().getNome(),
				"origem", Map.of("latitude", solicitacao.getOrigin().getLatitude(),
						"longitude", solicitacao.getOrigin().getLongitude()),
				"destino", Map.of("latitude", solicitacao.getDestination().getLatitude(),
						"longitude", solicitacao.getDestination().getLongitude()),
				"distanciaOrigemKm", fila.getDistanciaOrigemKm(),
				"tentativa", solicitacao.getTentativaAtual());
	}

	/**
	 * Notifica um motorista
	 */
	private void notificarMotorista(Long motoristaId, String eventName, Object data) {
		sseNotificationService.notificar(motoristaId, eventName, data);
	}

	/**
	 * Notifica um passageiro
	 */
	private void notificarPassageiro(Long passageiroId, String eventName, Object data) {
		sseNotificationService.notificar(passageiroId, eventName, data);
	}
}


