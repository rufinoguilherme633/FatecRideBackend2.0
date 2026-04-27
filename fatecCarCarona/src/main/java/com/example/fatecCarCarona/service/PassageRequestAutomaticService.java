package com.example.fatecCarCarona.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.DestinationDTO;
import com.example.fatecCarCarona.dto.NearbyDriversDTO;
import com.example.fatecCarCarona.dto.OpenstreetmapDTO;
import com.example.fatecCarCarona.dto.OriginDTO;
import com.example.fatecCarCarona.dto.PassageRequestsDTO;
import com.example.fatecCarCarona.dto.RouteCoordinatesDTO;
import com.example.fatecCarCarona.entity.City;
import com.example.fatecCarCarona.entity.Destination;
import com.example.fatecCarCarona.entity.Origin;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.PassageRequestQueue;
import com.example.fatecCarCarona.entity.PassageRequestQueueStatus;
import com.example.fatecCarCarona.entity.PassageRequestsPipelineStatus;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.PassageRequestQueueRepository;
import com.example.fatecCarCarona.repository.PassageRequestQueueStatusRepository;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.PassageRequestsPipelineStatusRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsável pelo novo fluxo automático de caronas
 * Sistema envia solicitação para motoristas próximos em cascata até uma aceitar
 */
@Service
@Slf4j
public class PassageRequestAutomaticService extends PassageRequestsService {

	@Autowired
	private PassageRequestQueueRepository filaRepository;

	@Autowired
	private PassageRequestQueueStatusRepository filaStatusRepository;

	@Autowired
	private PassageRequestsPipelineStatusRepository pipelineStatusRepository;

	@Autowired
	private PassageRequestsRepository passageRequestsRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FindNearbyDrivers findNearbyDrivers;

	@Autowired
	private RideRepository rideRepository;

	@Autowired
	private SseNotificationService sseNotificationService;

	@Autowired
	private PassageRequestsStatusService passageRequestsStatusService;

	@Autowired
	private OriginService originService;

	@Autowired
	private DestinationService destinationService;

	@Autowired
	private CityService cityService;

	/**
	 * Cria uma nova solicitação automática de carona
	 * 1. Valida endereços via ViaCep e OpenStreetMap
	 * 2. Cria Origin e Destination
	 * 3. Cria PassageRequests com status "aguardando_resposta"
	 * 4. Busca motoristas próximos e monta fila de cascata
	 * 5. Envia para o primeiro motorista
	 *
	 * @param dto DTO com origem, destino, etc
	 * @param passageiroId ID do passageiro
	 * @return PassageRequests criada
	 * @throws Exception se houver erro na validação ou busca de motoristas
	 */
	@Transactional(rollbackOn = Exception.class)
	public PassageRequests criarSolicitacaoAutomatica(PassageRequestsDTO dto, Long passageiroId) throws Exception {
		log.info("Criando solicitação automática para passageiro: {}", passageiroId);

		// 1. Busca usuário passageiro
		User passageiro = userRepository.findById(passageiroId)
			.orElseThrow(() -> new RuntimeException("Passageiro não encontrado"));

		// 2. Valida endereços
		validateAddress(dto.originDTO().cep(), dto.originDTO().cidade(),
			dto.originDTO().logradouro(), dto.originDTO().bairro());

		validateAddress(dto.destinationDTO().cep(), dto.destinationDTO().cidade(),
			dto.destinationDTO().logradouro(), dto.destinationDTO().bairro());

		// 3. Busca cidades
		City cidadeOrigem = cityService.validateCity(dto.originDTO().cidade());
		City cidadeDestino = cityService.validateCity(dto.destinationDTO().cidade());

		// 4. Busca coordenadas via OpenStreetMap
		String enderecoOrigem = String.format("%s %s", dto.originDTO().logradouro(), cidadeOrigem.getNome());
		String enderecoDestino = String.format("%s %s", dto.destinationDTO().logradouro(), cidadeDestino.getNome());

		OpenstreetmapDTO localizacaoOrigem = buscarLocalizacao(enderecoOrigem).get();
		OpenstreetmapDTO localizacaoDestino = buscarLocalizacao(enderecoDestino).get();

		// 5. Cria Origin e Destination
		Origin origem = criarOrigem(dto.originDTO(), cidadeOrigem, localizacaoOrigem);
		Destination destino = criarDestino(dto.destinationDTO(), cidadeDestino, localizacaoDestino);

		Origin origemSalva = originService.createOrigin(origem);
		Destination destinoSalvo = destinationService.createDestination(destino);

		// 6. Cria PassageRequests com status "aguardando_resposta"
		PassageRequests solicitacao = new PassageRequests();
		solicitacao.setPassageiro(passageiro);
		solicitacao.setOrigin(origemSalva);
		solicitacao.setDestination(destinoSalvo);
		solicitacao.setDataHora(LocalDateTime.now());
		solicitacao.setTentativaAtual(0);

		// Status = "aguardando_resposta" (fluxo automático)
		solicitacao.setStatus(passageRequestsStatusService.findByNome("aguardando_resposta"));

		// StatusPipeline = "aguardando"
		PassageRequestsPipelineStatus statusPipeline = pipelineStatusRepository
			.findByNome("aguardando")
			.orElseThrow(() -> new RuntimeException("Status 'aguardando' não encontrado"));
		solicitacao.setStatusPipeline(statusPipeline);

		solicitacao = passageRequestsRepository.save(solicitacao);

		log.info("Solicitação automática criada com ID: {}", solicitacao.getId());

		// 7. Busca motoristas próximos e monta fila
		try {
			buscarEMontarFila(solicitacao, dto);
			// 8. Envia para próximo motorista
			enviarParaProximoMotorista(solicitacao);
		} catch (Exception e) {
			log.error("Erro ao buscar motoristas ou enviar solicitação: {}", e.getMessage());
			declararFalhaFinal(solicitacao);
		}

		return solicitacao;
	}

	/**
	 * Busca motoristas próximos e monta a fila de cascata
	 * Cada motorista recebe um número de ordem (1, 2, 3)
	 *
	 * @param solicitacao PassageRequests criada
	 * @param dto DTO com dados da solicitação
	 * @throws Exception se nenhum motorista for encontrado
	 */
	@Transactional
	protected void buscarEMontarFila(PassageRequests solicitacao, PassageRequestsDTO dto) throws Exception {
		log.info("Buscando e montando fila de motoristas para solicitação: {}", solicitacao.getId());

		// 1. Monta RouteCoordinatesDTO com as coordenadas
		RouteCoordinatesDTO route = new RouteCoordinatesDTO(
			solicitacao.getOrigin().getLatitude(),
			solicitacao.getOrigin().getLongitude(),
			solicitacao.getDestination().getLatitude(),
			solicitacao.getDestination().getLongitude()
		);

		// 2. Busca motoristas próximos (já limitados a 3 e ordenados por score)
		List<NearbyDriversDTO> motoristasCandidatos = findNearbyDrivers.NearbyDriversService(route);

		if (motoristasCandidatos.isEmpty()) {
			throw new Exception("Nenhum motorista próximo encontrado");
		}

		// 3. Para cada motorista, cria entrada na fila
		int ordemFila = 1;
		for (NearbyDriversDTO motorista : motoristasCandidatos) {
			PassageRequestQueue entradaFila = new PassageRequestQueue();
			entradaFila.setSolicitacao(solicitacao);
			entradaFila.setMotorista(userRepository.findById(motorista.idMotorista())
				.orElseThrow(() -> new RuntimeException("Motorista não encontrado")));
			entradaFila.setRide(rideRepository.findById(motorista.idCarona())
				.orElseThrow(() -> new RuntimeException("Carona não encontrada")));
			entradaFila.setOrdemFila(ordemFila);

			// Busca o status "pendente" e seta na entrada
			PassageRequestQueueStatus statusPendente = filaStatusRepository
				.findByNome("pendente")
				.orElseThrow(() -> new RuntimeException("Status 'pendente' não encontrado"));
			entradaFila.setStatus(statusPendente);

			entradaFila.setDistanciaOrigemKm(motorista.distanciaOrigemKm());
			entradaFila.setDataEnvio(null); // Ainda não foi enviada

			filaRepository.save(entradaFila);
			log.info("Entrada de fila criada - Motorista: {}, Ordem: {}", motorista.idMotorista(), ordemFila);

			ordemFila++;
		}

		log.info("Fila montada com {} motoristas para solicitação {}", motoristasCandidatos.size(), solicitacao.getId());
	}

	/**
	 * Envia a solicitação para o próximo motorista na fila (status "pendente")
	 * Se não houver mais motoristas pendentes, declara falha final
	 *
	 * @param solicitacao PassageRequests
	 */
	@Transactional
	public void enviarParaProximoMotorista(PassageRequests solicitacao) {
		log.info("Enviando solicitação {} para próximo motorista", solicitacao.getId());

		// 1. Busca primeira entrada com status "pendente"
		Optional<PassageRequestQueue> proximaEntrada = filaRepository
			.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(solicitacao.getId(), "pendente");

		if (proximaEntrada.isEmpty()) {
			// Nenhum motorista pendente - declare falha final
			log.warn("Nenhum motorista pendente encontrado para solicitação {}", solicitacao.getId());
			declararFalhaFinal(solicitacao);
			return;
		}

		PassageRequestQueue entrada = proximaEntrada.get();

		// 2. Atualiza entrada: status = "enviada", dataEnvio = now()
		PassageRequestQueueStatus statusEnviada = filaStatusRepository
			.findByNome("enviada")
			.orElseThrow(() -> new RuntimeException("Status 'enviada' não encontrado"));

		entrada.setStatus(statusEnviada);
		entrada.setDataEnvio(LocalDateTime.now());
		filaRepository.save(entrada);

		// 3. Incrementa tentativa atual
		solicitacao.setTentativaAtual((solicitacao.getTentativaAtual() != null ? solicitacao.getTentativaAtual() : 0) + 1);
		passageRequestsRepository.save(solicitacao);

		// 4. Notifica motorista via SSE
		try {
			Object payloadMotorista = new Object() {
				public final Long id_solicitacao = solicitacao.getId();
				public final String nome_passageiro = solicitacao.getPassageiro().getNome();
				public final String foto_passageiro = solicitacao.getPassageiro().getFoto();
				public final String origem = solicitacao.getOrigin().getLogradouro() + ", " + solicitacao.getOrigin().getCity().getNome();
				public final String destino = solicitacao.getDestination().getLogradouro() + ", " + solicitacao.getDestination().getCity().getNome();
				public final Double distancia_km = entrada.getDistanciaOrigemKm();
				public final Long timeout_segundos = 120L;
			};

			sseNotificationService.notificar(entrada.getMotorista().getId(), "nova_solicitacao", payloadMotorista);
			log.info("SSE enviado para motorista {} - Solicitação {}", entrada.getMotorista().getId(), solicitacao.getId());
		} catch (Exception e) {
			log.error("Erro ao notificar motorista {}: {}", entrada.getMotorista().getId(), e.getMessage());
			// Não falha - o motorista pode estar offline
		}
	}

	/**
	 * Motorista aceita a solicitação que recebeu
	 * Atualiza status da fila e da solicitação
	 * Decrementa availableSeats da Ride
	 * Notifica passageiro
	 *
	 * @param solicitacaoId ID da solicitação
	 * @param motoristaId ID do motorista
	 */
	@Transactional
	public void motoristaAceita(Long solicitacaoId, Long motoristaId) {
		log.info("Motorista {} aceitando solicitação {}", motoristaId, solicitacaoId);

		// 1. Busca entrada com status "enviada"
		Optional<PassageRequestQueue> entradaOpt = filaRepository
			.findBySolicitacaoIdAndStatusNome(solicitacaoId, "enviada");

		if (entradaOpt.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada ou já foi respondida");
		}

		PassageRequestQueue entrada = entradaOpt.get();

		// 2. Valida que é o motorista correto
		if (!entrada.getMotorista().getId().equals(motoristaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista dessa solicitação");
		}

		// 3. Atualiza entrada: status = "aceita"
		PassageRequestQueueStatus statusAceita = filaStatusRepository
			.findByNome("aceita")
			.orElseThrow(() -> new RuntimeException("Status 'aceita' não encontrado"));

		entrada.setStatus(statusAceita);
		entrada.setDataResposta(LocalDateTime.now());
		filaRepository.save(entrada);

		// 4. Atualiza PassageRequests: status = "aceita"
		PassageRequests solicitacao = passageRequestsRepository.findById(solicitacaoId)
			.orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

		solicitacao.setStatus(passageRequestsStatusService.findByNome("aceita"));
		solicitacao.setCarona(entrada.getRide());

		PassageRequestsPipelineStatus statusPipelineAceita = pipelineStatusRepository
			.findByNome("aceita")
			.orElseThrow(() -> new RuntimeException("Status 'aceita' não encontrado"));
		solicitacao.setStatusPipeline(statusPipelineAceita);

		passageRequestsRepository.save(solicitacao);

		// 5. Decrementa availableSeats
		entrada.getRide().setAvailableSeats(entrada.getRide().getAvailableSeats() - 1);

		// 6. Notifica passageiro
		Object payloadPassageiro = new Object() {
			public final Long id_solicitacao = solicitacao.getId();
			public final String nome_motorista = entrada.getMotorista().getNome();
			public final String foto_motorista = entrada.getMotorista().getFoto();
			public final String telefone = entrada.getMotorista().getTelefone();
			public final String veiculo = entrada.getRide().getVehicle().getMarca() + " " + entrada.getRide().getVehicle().getModelo();
			public final String placa = entrada.getRide().getVehicle().getPlaca();
		};

		sseNotificationService.notificar(solicitacao.getPassageiro().getId(), "solicitacao_aceita", payloadPassageiro);
		log.info("Passageiro {} notificado - Solicitação aceita", solicitacao.getPassageiro().getId());
	}

	/**
	 * Motorista recusa a solicitação que recebeu
	 * Atualiza status da fila e tenta próximo motorista
	 *
	 * @param solicitacaoId ID da solicitação
	 * @param motoristaId ID do motorista
	 */
	@Transactional
	public void motoristaRecusa(Long solicitacaoId, Long motoristaId) {
		log.info("Motorista {} recusando solicitação {}", motoristaId, solicitacaoId);

		// 1. Busca entrada com status "enviada"
		Optional<PassageRequestQueue> entradaOpt = filaRepository
			.findBySolicitacaoIdAndStatusNome(solicitacaoId, "enviada");

		if (entradaOpt.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação não encontrada ou já foi respondida");
		}

		PassageRequestQueue entrada = entradaOpt.get();

		// 2. Valida que é o motorista correto
		if (!entrada.getMotorista().getId().equals(motoristaId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não é o motorista dessa solicitação");
		}

		// 3. Atualiza entrada: status = "recusada"
		PassageRequestQueueStatus statusRecusada = filaStatusRepository
			.findByNome("recusada")
			.orElseThrow(() -> new RuntimeException("Status 'recusada' não encontrado"));

		entrada.setStatus(statusRecusada);
		entrada.setDataResposta(LocalDateTime.now());
		filaRepository.save(entrada);

		// 4. Tenta próximo motorista
		PassageRequests solicitacao = passageRequestsRepository.findById(solicitacaoId)
			.orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

		tentarProximo(solicitacao);
	}

	/**
	 * Verifica se há motoristas pendentes e envia para o próximo
	 * Se não houver mais, declara falha final
	 *
	 * @param solicitacao PassageRequests
	 */
	@Transactional
	public void tentarProximo(PassageRequests solicitacao) {
		log.info("Tentando próximo motorista para solicitação {}", solicitacao.getId());

		// 1. Verifica se há motoristas pendentes
		Optional<PassageRequestQueue> proximaPendente = filaRepository
			.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(solicitacao.getId(), "pendente");

		if (proximaPendente.isPresent()) {
			// 2. Se sim: envia para próximo
			enviarParaProximoMotorista(solicitacao);
		} else {
			// 3. Se não: declara falha
			declararFalhaFinal(solicitacao);
		}
	}

	/**
	 * Declara falha final da solicitação
	 * Nenhum motorista aceitou nem há mais para tentar
	 * Notifica passageiro
	 *
	 * @param solicitacao PassageRequests
	 */
	@Transactional
	public void declararFalhaFinal(PassageRequests solicitacao) {
		log.warn("Falha final na solicitação {}", solicitacao.getId());

		// 1. Atualiza PassageRequests: status = "falha_final"
		solicitacao.setStatus(passageRequestsStatusService.findByNome("falha_final"));

		PassageRequestsPipelineStatus statusPipelineFalha = pipelineStatusRepository
			.findByNome("falha_final")
			.orElseThrow(() -> new RuntimeException("Status 'falha_final' não encontrado"));
		solicitacao.setStatusPipeline(statusPipelineFalha);

		passageRequestsRepository.save(solicitacao);

		// 2. Notifica passageiro
		Object payloadPassageiro = new Object() {
			public final String mensagem = "Nenhum motorista disponível no momento. Tente novamente mais tarde.";
			public final boolean pode_tentar_novamente = true;
		};

		sseNotificationService.notificar(solicitacao.getPassageiro().getId(), "falha_final", payloadPassageiro);
		log.info("Passageiro {} notificado - Falha final", solicitacao.getPassageiro().getId());
	}

	/**
	 * Permite que o passageiro faça retry após falha final
	 * Cria nova solicitação reutilizando mesmos Origin e Destination
	 * Monta nova fila e inicia novo ciclo
	 *
	 * @param solicitacaoOriginalId ID da solicitação original (que falhou)
	 * @param passageiroId ID do passageiro
	 * @return Nova PassageRequests criada
	 */
	@Transactional
	public PassageRequests retryComMesmosDados(Long solicitacaoOriginalId, Long passageiroId) {
		log.info("Iniciando retry da solicitação {} para passageiro {}", solicitacaoOriginalId, passageiroId);

		// 1. Busca solicitação original
		PassageRequests solicitacaoOriginal = passageRequestsRepository.findById(solicitacaoOriginalId)
			.orElseThrow(() -> new RuntimeException("Solicitação não encontrada"));

		// 2. Valida que pertence ao passageiro
		if (!solicitacaoOriginal.getPassageiro().getId().equals(passageiroId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solicitação não pertence a este passageiro");
		}

		// 3. Valida que o status é "falha_final"
		if (!solicitacaoOriginal.getStatus().getNome().equals("falha_final")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas solicitações com falha final podem fazer retry");
		}

		// 4. Cria nova solicitação reutilizando Origin e Destination
		PassageRequests novaSolicitacao = new PassageRequests();
		novaSolicitacao.setPassageiro(solicitacaoOriginal.getPassageiro());
		novaSolicitacao.setOrigin(solicitacaoOriginal.getOrigin());
		novaSolicitacao.setDestination(solicitacaoOriginal.getDestination());
		novaSolicitacao.setDataHora(LocalDateTime.now());
		novaSolicitacao.setTentativaAtual(0);
		novaSolicitacao.setStatus(passageRequestsStatusService.findByNome("aguardando_resposta"));

		PassageRequestsPipelineStatus statusPipeline = pipelineStatusRepository
			.findByNome("aguardando")
			.orElseThrow(() -> new RuntimeException("Status 'aguardando' não encontrado"));
		novaSolicitacao.setStatusPipeline(statusPipeline);

		novaSolicitacao = passageRequestsRepository.save(novaSolicitacao);

		log.info("Nova solicitação criada para retry com ID: {}", novaSolicitacao.getId());

		// 5. Monta nova fila
		try {
			// Reconstrói PassageRequestsDTO com dados da solicitação original
			OriginDTO originDTO = new OriginDTO(
				solicitacaoOriginal.getOrigin().getCity().getNome(),
				solicitacaoOriginal.getOrigin().getLogradouro(),
				solicitacaoOriginal.getOrigin().getNumero(),
				solicitacaoOriginal.getOrigin().getBairro(),
				solicitacaoOriginal.getOrigin().getCep()
			);

			DestinationDTO destinationDTO = new DestinationDTO(
				solicitacaoOriginal.getDestination().getCity().getNome(),
				solicitacaoOriginal.getDestination().getLogradouro(),
				solicitacaoOriginal.getDestination().getNumero(),
				solicitacaoOriginal.getDestination().getBairro(),
				solicitacaoOriginal.getDestination().getCep()
			);

			PassageRequestsDTO dto = new PassageRequestsDTO(originDTO, destinationDTO, null);

			buscarEMontarFila(novaSolicitacao, dto);
			// Inicia novo fluxo
			enviarParaProximoMotorista(novaSolicitacao);
		} catch (Exception e) {
			log.error("Erro ao fazer retry: {}", e.getMessage());
			declararFalhaFinal(novaSolicitacao);
		}

		return novaSolicitacao;
	}
}









