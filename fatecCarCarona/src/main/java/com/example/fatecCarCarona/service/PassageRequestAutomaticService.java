package com.example.fatecCarCarona.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationContext;
import lombok.extern.slf4j.Slf4j;

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
	private PassageRequestsStatusService passageRequestsStatusService;

	@Value("${carona.auto.timeout-motorista-segundos:60}")
	private Integer timeoutMotoristaSegundos;

	@Value("${carona.auto.limite-tentativas:3}")
	private Integer limiteTentativas;

	// ScheduledExecutorService para gerenciar timeouts
	private final ScheduledExecutorService timeoutExecutor = Executors.newScheduledThreadPool(2);

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

		// Buscar motoristas próximos
		List<NearbyDriversDTO> motoristasProximos = findNearbyDrivers.NearbyDriversService(
				new RouteCoordinatesDTO(latitudeOrigem, longitudeOrigem, latitudeDestino, longitudeDestino));

		if (motoristasProximos == null || motoristasProximos.isEmpty()) {
			log.warn("Nenhum motorista próximo encontrado para solicitação: {}", passageRequestId);
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
			var motoristaUser = userRepository.findById(motorista.idMotorista())
					.orElseThrow(() -> new Exception("Motorista candidato não encontrado: " + motorista.idMotorista()));
			filaEntry.setMotorista(motoristaUser);
			filaEntry.setRide(solicitacao.getCarona());
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
		if (solicitacao.getTentativaAtual() >= limiteTentativas) {
			log.warn("Limite de tentativas atingido para solicitação: {}", solicitacao.getId());
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

		// Configurar timeout para esta tentativa
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

		// Atualizar status da entrada de fila para "aceita"
		PassageRequestQueueStatus statusAceita = queueStatusRepository.findByNome("aceita")
				.orElseThrow(() -> new Exception("Status 'aceita' não encontrado"));
		fila.setStatus(statusAceita);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

		// Atualizar pipeline da solicitação para 'aceita'
		atualizarStatusPipeline(solicitacao, "aceita");

		// Atualizar status da solicitação como aceita
		solicitacao.setStatus(passageRequestsStatusService.findByNome("aceita"));
		passageRequestsRepository.save(solicitacao);

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

		// Atualizar status para "recusada"
		PassageRequestQueueStatus statusRecusada = queueStatusRepository.findByNome("recusada")
				.orElseThrow(() -> new Exception("Status 'recusada' não encontrado"));
		fila.setStatus(statusRecusada);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

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

		// Atualizar status para "timeout"
		PassageRequestQueueStatus statusTimeout = queueStatusRepository.findByNome("timeout")
				.orElseThrow(() -> new Exception("Status 'timeout' não encontrado"));
		fila.setStatus(statusTimeout);
		fila.setDataResposta(LocalDateTime.now());
		passageRequestQueueRepository.save(fila);

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

		timeoutExecutor.schedule(() -> {
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
			}
		}, timeoutMotoristaSegundos, TimeUnit.SECONDS);
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


