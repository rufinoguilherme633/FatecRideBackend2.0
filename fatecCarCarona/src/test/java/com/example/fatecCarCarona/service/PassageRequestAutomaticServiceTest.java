package com.example.fatecCarCarona.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.fatecCarCarona.dto.NearbyDriversDTO;
import com.example.fatecCarCarona.dto.RouteCoordinatesDTO;
import com.example.fatecCarCarona.entity.Destination;
import com.example.fatecCarCarona.entity.Origin;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.PassageRequestQueue;
import com.example.fatecCarCarona.entity.PassageRequestQueueStatus;
import com.example.fatecCarCarona.entity.PassageRequestsPipelineStatus;
import com.example.fatecCarCarona.entity.PassageRequestsStatus;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.PassageRequestQueueRepository;
import com.example.fatecCarCarona.repository.PassageRequestQueueStatusRepository;
import com.example.fatecCarCarona.repository.PassageRequestsPipelineStatusRepository;
import com.example.fatecCarCarona.repository.PassageRequestsRepository;
import com.example.fatecCarCarona.repository.RideRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("PassageRequestAutomaticService - Testes Unitários")
class PassageRequestAutomaticServiceTest {

	@Mock
	private PassageRequestsRepository passageRequestsRepository;

	@Mock
	private PassageRequestQueueRepository passageRequestQueueRepository;

	@Mock
	private PassageRequestQueueStatusRepository queueStatusRepository;

	@Mock
	private PassageRequestsPipelineStatusRepository pipelineStatusRepository;

	@Mock
	private FindNearbyDrivers findNearbyDrivers;

	@Mock
	private SseNotificationService sseNotificationService;

	@Mock
	private PassageRequestsStatusService passageRequestsStatusService;

	@Mock
	private com.example.fatecCarCarona.repository.UserRepository userRepository;

	@Mock
	private RideRepository rideRepository;

	@InjectMocks
	private PassageRequestAutomaticService service;

	private PassageRequests solicitation;
	private User driver;
	private User passenger;
	private Ride ride;
	private Origin origin;
	private Destination destination;
	private PassageRequestQueueStatus statusPendente;
	private PassageRequestQueueStatus statusEnviada;
	private PassageRequestQueueStatus statusAceita;
	private PassageRequestQueueStatus statusRecusada;
	private PassageRequestQueueStatus statusTimeout;
	private PassageRequestsPipelineStatus statusAguardando;
	private PassageRequestsPipelineStatus statusAceitaPipeline;
	private PassageRequestsPipelineStatus statusFalhaFinal;
	private PassageRequestsStatus statusSolicitacaoPendente;
	private PassageRequestsStatus statusSolicitacaoAceita;
	private PassageRequestsStatus statusSolicitacaoRecusada;

	@BeforeEach
	void setUp() {
		// Configurar valores padrão para os testes
		ReflectionTestUtils.setField(service, "timeoutMotoristaSegundos", 60);
		ReflectionTestUtils.setField(service, "limiteTentativas", 3);

		// Criar localidades
		origin = new Origin();
		origin.setId(1L);
		origin.setLatitude(-23.5505);
		origin.setLongitude(-46.6333);

		destination = new Destination();
		destination.setId(1L);
		destination.setLatitude(-23.5710);
		destination.setLongitude(-46.6560);

		// Criar motorista
		driver = new User();
		driver.setId(1L);
		driver.setNome("João");
		driver.setEmail("joao@example.com");

		// Criar passageiro
		passenger = new User();
		passenger.setId(2L);
		passenger.setNome("Maria");
		passenger.setEmail("maria@example.com");

		// Criar carona
		ride = new Ride();
		ride.setId(1L);
		ride.setDriver(driver);
		ride.setOrigin(origin);
		ride.setDestination(destination);

		// Criar status da fila
		statusPendente = new PassageRequestQueueStatus();
		statusPendente.setId(1L);
		statusPendente.setNome("pendente");

		statusEnviada = new PassageRequestQueueStatus();
		statusEnviada.setId(2L);
		statusEnviada.setNome("enviada");

		statusAceita = new PassageRequestQueueStatus();
		statusAceita.setId(3L);
		statusAceita.setNome("aceita");

		statusRecusada = new PassageRequestQueueStatus();
		statusRecusada.setId(4L);
		statusRecusada.setNome("recusada");

		statusTimeout = new PassageRequestQueueStatus();
		statusTimeout.setId(5L);
		statusTimeout.setNome("timeout");

		// Criar status do pipeline
		statusAguardando = new PassageRequestsPipelineStatus();
		statusAguardando.setId(1L);
		statusAguardando.setNome("aguardando");

		statusAceitaPipeline = new PassageRequestsPipelineStatus();
		statusAceitaPipeline.setId(2L);
		statusAceitaPipeline.setNome("aceita");

		statusFalhaFinal = new PassageRequestsPipelineStatus();
		statusFalhaFinal.setId(3L);
		statusFalhaFinal.setNome("falha_final");

		// Criar status da solicitação
		statusSolicitacaoPendente = new PassageRequestsStatus();
		statusSolicitacaoPendente.setId(1L);
		statusSolicitacaoPendente.setNome("pendente");

		statusSolicitacaoAceita = new PassageRequestsStatus();
		statusSolicitacaoAceita.setId(2L);
		statusSolicitacaoAceita.setNome("aceita");

		statusSolicitacaoRecusada = new PassageRequestsStatus();
		statusSolicitacaoRecusada.setId(3L);
		statusSolicitacaoRecusada.setNome("recusada");

		// Criar solicitação
		solicitation = new PassageRequests();
		solicitation.setId(1L);
		solicitation.setCarona(ride);
		solicitation.setPassageiro(passenger);
		solicitation.setOrigin(origin);
		solicitation.setDestination(destination);
		solicitation.setStatus(statusSolicitacaoPendente);
		solicitation.setTentativaAtual(0);

		// Mock padrão leniente para userRepository.findById usado na criação de filas
		lenient().when(userRepository.findById(anyLong())).thenAnswer(invocation -> {
			Long id = invocation.getArgument(0);
			User u = new User();
			u.setId(id);
			u.setNome("Driver" + id);
			u.setEmail("driver" + id + "@example.com");
			return Optional.of(u);
		});

		lenient().when(rideRepository.findById(anyLong())).thenAnswer(invocation -> {
			Long id = invocation.getArgument(0);
			Ride r = new Ride();
			r.setId(id);
			r.setDriver(driver);
			r.setOrigin(origin);
			r.setDestination(destination);
			return Optional.of(r);
		});
	}

	@Test
	@DisplayName("Deve iniciar fluxo automático com sucesso")
	void testIniciarFluxoAutomatico_Success() throws Exception {
		// Arrange
		List<NearbyDriversDTO> driversFound = criarMotoristasMockados(3);

		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(findNearbyDrivers.NearbyDriversService(any(RouteCoordinatesDTO.class)))
				.thenReturn(driversFound);
		when(pipelineStatusRepository.findByNome("aguardando"))
				.thenReturn(Optional.of(statusAguardando));
		when(queueStatusRepository.findByNome("pendente"))
				.thenReturn(Optional.of(statusPendente));
		when(queueStatusRepository.findByNome("enviada"))
				.thenReturn(Optional.of(statusEnviada));
		when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L))
				.thenReturn(new ArrayList<>());
		when(passageRequestQueueRepository.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(1L, "pendente"))
				.thenReturn(Optional.of(criarFilaMotorista(1L, statusPendente)));

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.iniciarFluxoAutomatico(1L, -23.5505, -46.6333, -23.5710, -46.6560);
		});

		verify(passageRequestsRepository, times(2)).save(any(PassageRequests.class));
		verify(sseNotificationService, times(1)).notificar(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("Deve lançar exceção quando solicitação não encontrada")
	void testIniciarFluxoAutomatico_SolicitacaoNaoEncontrada() {
		// Arrange
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(Exception.class, () -> {
			service.iniciarFluxoAutomatico(1L, -23.5505, -46.6333, -23.5710, -46.6560);
		});

		verify(passageRequestsRepository, times(1)).findById(1L);
	}

	@Test
	@DisplayName("Deve processar aceitação de motorista com sucesso")
	void testHandleMotoristaAceita_Success() throws Exception {
		// Arrange
		PassageRequestQueue fila = criarFilaMotorista(1L, statusEnviada);

		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila));
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(queueStatusRepository.findByNome("aceita")).thenReturn(Optional.of(statusAceita));
		when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(statusRecusada));
		when(pipelineStatusRepository.findByNome("aceita"))
				.thenReturn(Optional.of(statusAceitaPipeline));
		when(passageRequestsStatusService.findByNome("aceita"))
				.thenReturn(statusSolicitacaoAceita);
		when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L))
				.thenReturn(List.of(fila));

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.handleMotoristaAceita(1L, 1L, 1L);
		});

		verify(passageRequestQueueRepository, times(1)).save(any(PassageRequestQueue.class));
		verify(passageRequestsRepository, atLeast(1)).save(any(PassageRequests.class));
		verify(sseNotificationService, atLeast(1)).notificar(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("Deve processar recusa de motorista com sucesso")
	void testHandleMotoristaRecusa_Success() throws Exception {
		// Arrange
		PassageRequestQueue fila = criarFilaMotorista(1L, statusEnviada);
		PassageRequestQueue proximaFila = criarFilaMotorista(2L, statusPendente);

		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila));
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(statusRecusada));
		when(queueStatusRepository.findByNome("enviada")).thenReturn(Optional.of(statusEnviada));
		when(passageRequestQueueRepository.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(1L, "pendente"))
				.thenReturn(Optional.of(proximaFila));

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.handleMotoristaRecusa(1L, 1L, 1L);
		});

		verify(passageRequestQueueRepository, times(2)).save(any(PassageRequestQueue.class));
		verify(sseNotificationService, times(1)).notificar(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("Deve processar timeout de motorista com sucesso")
	void testHandleTimeoutMotorista_Success() throws Exception {
		// Arrange
		PassageRequestQueue fila = criarFilaMotorista(1L, statusEnviada);
		PassageRequestQueue proximaFila = criarFilaMotorista(2L, statusPendente);

		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila));
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(queueStatusRepository.findByNome("timeout")).thenReturn(Optional.of(statusTimeout));
		when(queueStatusRepository.findByNome("enviada")).thenReturn(Optional.of(statusEnviada));
		when(passageRequestQueueRepository.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(1L, "pendente"))
				.thenReturn(Optional.of(proximaFila));

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.handleTimeoutMotorista(1L, 1L);
		});

		verify(passageRequestQueueRepository, times(2)).save(any(PassageRequestQueue.class));
		verify(sseNotificationService, times(1)).notificar(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("Deve atingir limite de tentativas e finalizar com falha")
	void testEnviarProximoMotorista_LimiteTentativasAtingido() throws Exception {
		// Arrange
		solicitation.setTentativaAtual(3); // Limite já atingido
		when(pipelineStatusRepository.findByNome("falha_final"))
				.thenReturn(Optional.of(statusFalhaFinal));
		when(passageRequestsStatusService.findByNome("recusada"))
				.thenReturn(statusSolicitacaoRecusada);
		when(passageRequestsRepository.save(any(PassageRequests.class)))
				.thenReturn(solicitation);

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.enviarProximoMotorista(solicitation);
		});

		// Agora há 2 saves: um em marcarSolicitacaoComoRecusada() e outro em atualizarStatusPipeline()
		verify(passageRequestsRepository, times(2)).save(any(PassageRequests.class));
		verify(sseNotificationService, times(1)).notificar(anyLong(), anyString(), any());
	}

	@Test
	@DisplayName("Deve lançar exceção quando motorista não está na fila")
	void testHandleMotoristaAceita_FilaNaoEncontrada() {
		// Arrange
		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(Exception.class, () -> {
			service.handleMotoristaAceita(1L, 1L, 1L);
		});

		verify(passageRequestQueueRepository, times(1)).findById(1L);
	}

	@Test
	@DisplayName("Deve notificar passageiro quando nenhum motorista encontrado")
	void testIniciarFluxoAutomatico_NenhumMotoristaPróximo() throws Exception {
		// Arrange
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(findNearbyDrivers.NearbyDriversService(any(RouteCoordinatesDTO.class)))
				.thenReturn(new ArrayList<>()); // Lista vazia
		when(pipelineStatusRepository.findByNome("falha_final"))
				.thenReturn(Optional.of(statusFalhaFinal));
		when(passageRequestsStatusService.findByNome("recusada"))
				.thenReturn(statusSolicitacaoRecusada);
		when(passageRequestsRepository.save(any(PassageRequests.class)))
				.thenReturn(solicitation);

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.iniciarFluxoAutomatico(1L, -23.5505, -46.6333, -23.5710, -46.6560);
		});

		verify(sseNotificationService, times(1)).notificar(eq(2L), eq("nenhum_motorista"), any());
	}

	@Test
	@DisplayName("Deve lançar exceção quando nenhum motorista pendente encontrado")
	void testEnviarProximoMotorista_NenhumPendente() throws Exception {
		// Arrange
		solicitation.setTentativaAtual(0);
		when(passageRequestQueueRepository.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(1L, "pendente"))
				.thenReturn(Optional.empty()); // Nenhuma fila pendente
		when(pipelineStatusRepository.findByNome("falha_final"))
				.thenReturn(Optional.of(statusFalhaFinal));
		when(passageRequestsStatusService.findByNome("recusada"))
				.thenReturn(statusSolicitacaoRecusada);
		when(passageRequestsRepository.save(any(PassageRequests.class)))
				.thenReturn(solicitation);

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.enviarProximoMotorista(solicitation);
		});

		verify(pipelineStatusRepository, times(1)).findByNome("falha_final");
		verify(sseNotificationService, times(1)).notificar(eq(2L), eq("falha_final"), any());
	}

	@Test
	@DisplayName("Deve rejeitar múltiplos motoristas quando um aceita")
	void testRejeitarOutrosMotoristas_MultiplosMotoristas() throws Exception {
		// Arrange
		PassageRequestQueue fila1 = criarFilaMotorista(1L, statusAceita);

		// Criar motoristas diferentes para fila2 e fila3
		User driver2 = new User();
		driver2.setId(2L);
		driver2.setNome("Pedro");
		driver2.setEmail("pedro@example.com");

		User driver3 = new User();
		driver3.setId(3L);
		driver3.setNome("Ana");
		driver3.setEmail("ana@example.com");

		PassageRequestQueue fila2 = new PassageRequestQueue();
		fila2.setId(2L);
		fila2.setSolicitacao(solicitation);
		fila2.setMotorista(driver2);
		fila2.setRide(ride);
		fila2.setOrdemFila(2);
		fila2.setStatus(statusEnviada);
		fila2.setDataEnvio(LocalDateTime.now());
		fila2.setDistanciaOrigemKm(2.0);

		PassageRequestQueue fila3 = new PassageRequestQueue();
		fila3.setId(3L);
		fila3.setSolicitacao(solicitation);
		fila3.setMotorista(driver3);
		fila3.setRide(ride);
		fila3.setOrdemFila(3);
		fila3.setStatus(statusEnviada);
		fila3.setDataEnvio(LocalDateTime.now());
		fila3.setDistanciaOrigemKm(3.0);

		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila1));
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(queueStatusRepository.findByNome("aceita")).thenReturn(Optional.of(statusAceita));
		when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(statusRecusada));
		when(pipelineStatusRepository.findByNome("aceita"))
				.thenReturn(Optional.of(statusAceitaPipeline));
		when(passageRequestsStatusService.findByNome("aceita"))
				.thenReturn(statusSolicitacaoAceita);
		when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L))
				.thenReturn(List.of(fila1, fila2, fila3));

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.handleMotoristaAceita(1L, 1L, 1L);
		});

		// Deve rejeitar 2 motoristas (fila2 e fila3) + atualizar fila1
		verify(passageRequestQueueRepository, times(3)).save(any(PassageRequestQueue.class));
	}

	@Test
	@DisplayName("Deve processar timeout quando motorista não respondeu dentro do prazo")
	void testAgendarTimeoutMotorista_ComResposta() throws Exception {
		// Arrange
		PassageRequestQueue fila = criarFilaMotorista(1L, statusEnviada);

		when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila));
		when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
		when(queueStatusRepository.findByNome("timeout")).thenReturn(Optional.of(statusTimeout));
		when(queueStatusRepository.findByNome("enviada")).thenReturn(Optional.of(statusEnviada));
		// Retornar próxima fila pendente para enviar
		PassageRequestQueue proximaFila = criarFilaMotorista(2L, statusPendente);
		when(passageRequestQueueRepository.findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(1L, "pendente"))
				.thenReturn(Optional.of(proximaFila));
		when(passageRequestsRepository.save(any(PassageRequests.class))).thenReturn(solicitation);

		// Act & Assert
		assertDoesNotThrow(() -> {
			service.handleTimeoutMotorista(1L, 1L);
		});

		// Deve fazer: timeout na fila1 + atualizar fila2 para enviada
		verify(passageRequestQueueRepository, times(2)).save(any(PassageRequestQueue.class));
	}

	// ===== MÉTODOS AUXILIARES =====

	private List<NearbyDriversDTO> criarMotoristasMockados(int quantidade) {
		List<NearbyDriversDTO> drivers = new ArrayList<>();
		for (int i = 0; i < quantidade; i++) {
			drivers.add(new NearbyDriversDTO(
					1L + i, // idCarona
					1L + i, // idMotorista
					"Motorista " + i, // nome
					"Sobrenome " + i, // sobrenome
					"driver" + i + "@example.com", // email
					"11999999" + i, // telefone
					null, // foto
					"M", // genero
					"Engenharia", // curso
					"São Paulo", // cidadeOrigem
					"Rua A", // logradouroOrigem
					"Vila A", // bairroOrigem
					-23.5505, // latitudeOrigem
					-46.6333, // longitudeOrigem
					1.5 + i, // distanciaOrigemKm
					"São Paulo", // cidadeDestino
					"Rua B", // logradouroDestino
					"Vila B", // bairroDestino
					-23.5710, // latitudeDestino
					-46.6560, // longitudeDestino
					"Honda", // modeloCarro
					"Civic", // marcaCarro
					"ABC-1234", // placaCarro
					"Preto", // corCarro
					2020, // anoCarro
					3, // vagasDisponiveis
					0 // vagasOcupadas
			));
		}
		return drivers;
	}

	private PassageRequestQueue criarFilaMotorista(Long id, PassageRequestQueueStatus status) {
		PassageRequestQueue fila = new PassageRequestQueue();
		fila.setId(id);
		fila.setSolicitacao(solicitation);
		fila.setMotorista(driver);
		fila.setRide(ride);
		fila.setOrdemFila(id.intValue());
		fila.setStatus(status);
		fila.setDataEnvio(LocalDateTime.now());
		fila.setDistanciaOrigemKm(1.5);
		return fila;
	}
}






