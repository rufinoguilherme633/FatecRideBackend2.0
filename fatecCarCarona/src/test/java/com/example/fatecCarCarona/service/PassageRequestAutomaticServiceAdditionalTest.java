package com.example.fatecCarCarona.service;

import com.example.fatecCarCarona.dto.NearbyDriversDTO;
import com.example.fatecCarCarona.entity.*;
import com.example.fatecCarCarona.repository.*;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassageRequestAutomaticServiceAdditionalTest {

    @Mock
    private PassageRequestsRepository passageRequestsRepository;

    @Mock
    private PassageRequestQueueRepository passageRequestQueueRepository;

    @Mock
    private PassageRequestQueueStatusRepository queueStatusRepository;

    @Mock
    private com.example.fatecCarCarona.repository.UserRepository userRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private PassageRequestsPipelineStatusRepository pipelineStatusRepository;

    @Mock
    private SseNotificationService sseNotificationService;

    @Mock
    private PassageRequestsStatusService passageRequestsStatusService;

    @InjectMocks
    private PassageRequestAutomaticService service;

    private PassageRequests solicitation;
    private User user;
    private Ride ride;
    private PassageRequestQueueStatus statusPendente;
    private PassageRequestsPipelineStatus statusAguardando;
    private PassageRequestsStatus statusSolicitacaoAceita;

    @BeforeEach
    void setUp() {
        solicitation = new PassageRequests();
        solicitation.setId(1L);
        user = new User();
        user.setId(100L);
        ride = new Ride();
        ride.setId(200L);

        // garantir passageiro e coordenadas mínimas na solicitação para evitar NPEs em notificações
        solicitation.setPassageiro(user);
        com.example.fatecCarCarona.entity.Origin origin = new com.example.fatecCarCarona.entity.Origin();
        origin.setLatitude(-23.55);
        origin.setLongitude(-46.63);
        solicitation.setOrigin(origin);
        com.example.fatecCarCarona.entity.Destination destination = new com.example.fatecCarCarona.entity.Destination();
        destination.setLatitude(-23.57);
        destination.setLongitude(-46.65);
        solicitation.setDestination(destination);

        statusPendente = new PassageRequestQueueStatus();
        statusPendente.setId(1L);
        statusPendente.setNome("pendente");

        statusAguardando = new PassageRequestsPipelineStatus();
        statusAguardando.setId(1L);
        statusAguardando.setNome("aguardando");

        statusSolicitacaoAceita = new PassageRequestsStatus();
        statusSolicitacaoAceita.setId(2L);
        statusSolicitacaoAceita.setNome("aceita");

        // default stubs
        when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(anyLong()))
                .thenReturn(new ArrayList<>());
        when(queueStatusRepository.findByNome("pendente")).thenReturn(Optional.of(statusPendente));
    }

    @Test
    @DisplayName("criarFilaMotoristas deve pular candidatos inválidos e salvar apenas válidos")
    void testCriarFilaMotoristas_SkipsInvalidCandidates() throws Exception {
        // montar candidatos: 1) motorista inexistente, 2) idCarona null, 3) ride inexistente, 4) válido
        List<NearbyDriversDTO> candidatos = new ArrayList<>();

        // 1) motorista inexistente -> userRepository.findById -> empty
        candidatos.add(new NearbyDriversDTO(300L, 10L, "n1", "sn", "e", "t", null, "M", "C", "c", "l", "b", -23.5, -46.6, 1.0, "dest", "lg", "br", -23.6, -46.7, "mod", "marca", "ABC", "preto", 2020, 2, 0));

        // 2) idCarona null
        candidatos.add(new NearbyDriversDTO(null, 11L, "n2", "sn", "e", "t", null, "M", "C", "c", "l", "b", -23.5, -46.6, 2.0, "dest", "lg", "br", -23.6, -46.7, "mod", "marca", "ABC", "preto", 2020, 2, 0));

        // 3) ride inexistente (rideRepository empty)
        candidatos.add(new NearbyDriversDTO(301L, 12L, "n3", "sn", "e", "t", null, "M", "C", "c", "l", "b", -23.5, -46.6, 3.0, "dest", "lg", "br", -23.6, -46.7, "mod", "marca", "ABC", "preto", 2020, 2, 0));

        // 4) válido
        candidatos.add(new NearbyDriversDTO(200L, 13L, "n4", "sn", "e", "t", null, "M", "C", "c", "l", "b", -23.5, -46.6, 4.0, "dest", "lg", "br", -23.6, -46.7, "mod", "marca", "ABC", "preto", 2020, 2, 0));

        // stubs
        when(userRepository.findById(10L)).thenReturn(Optional.empty());
        when(userRepository.findById(11L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(12L)).thenReturn(Optional.of(new User()));
        when(userRepository.findById(13L)).thenReturn(Optional.of(new User()));
        // ride id 301L -> return empty to simulate inexistente
        when(rideRepository.findById(301L)).thenReturn(Optional.empty());
        // ride id 200L -> valid
        when(rideRepository.findById(200L)).thenReturn(Optional.of(ride));

        // execute
        service.criarFilaMotoristas(solicitation, candidatos);

        // capture saves
        ArgumentCaptor<PassageRequestQueue> captor = ArgumentCaptor.forClass(PassageRequestQueue.class);
        verify(passageRequestQueueRepository, times(1)).save(captor.capture());

        PassageRequestQueue salvo = captor.getValue();
        assertNotNull(salvo);
        assertNotNull(salvo.getRide());
        assertEquals(ride.getId(), salvo.getRide().getId());
        assertEquals(1, salvo.getOrdemFila());
        assertEquals(4.0, salvo.getDistanciaOrigemKm());
    }

    @Test
    @DisplayName("handleMotoristaAceita deve traduzir OptimisticLockException em IllegalStateException")
    void testHandleMotoristaAceita_OptimisticLock() throws Exception {
        // preparar fila e solicitacao
        PassageRequestQueue fila = new PassageRequestQueue();
        fila.setId(1L);
        fila.setSolicitacao(solicitation);
        User motorista = new User();
        motorista.setId(5L);
        fila.setMotorista(motorista);
        PassageRequestQueueStatus enviada = new PassageRequestQueueStatus();
        enviada.setNome("enviada");
        fila.setStatus(enviada);

        // solicitation with pipeline aguardando
        solicitation.setStatusPipeline(statusAguardando);

        // stubs
        when(passageRequestQueueRepository.findById(1L)).thenReturn(Optional.of(fila));
        when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
        when(queueStatusRepository.findByNome("aceita")).thenReturn(Optional.of(new PassageRequestQueueStatus()));
        when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(new PassageRequestQueueStatus()));
        when(pipelineStatusRepository.findByNome("aceita")).thenReturn(Optional.of(new PassageRequestsPipelineStatus()));
        when(passageRequestsStatusService.findByNome("aceita")).thenReturn(statusSolicitacaoAceita);

        // passageRequestQueueRepository.save should work for fila
        when(passageRequestQueueRepository.save(any(PassageRequestQueue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // passageRequestsRepository.save: first save (from atualizarStatusPipeline) -> return solicitation
        // second save -> throw OptimisticLockException
        AtomicInteger counter = new AtomicInteger(0);
        when(passageRequestsRepository.save(any(PassageRequests.class))).thenAnswer(invocation -> {
            int c = counter.getAndIncrement();
            if (c == 0) {
                return invocation.getArgument(0);
            }
            throw new OptimisticLockException("conflict");
        });

        // realizar
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            service.handleMotoristaAceita(1L, 1L, motorista.getId());
        });

        assertTrue(ex.getMessage().contains("Solicitação já processada"));

        // verify saves: queue saved at least once, and repository.save was called at least twice (pipeline + attempted accept)
        verify(passageRequestQueueRepository, atLeastOnce()).save(any(PassageRequestQueue.class));
        verify(passageRequestsRepository, atLeast(2)).save(any(PassageRequests.class));
    }

    @Test
    @DisplayName("Ao aceitar solicitação, a fila deve ser limpa")
    void testFilaLimpa_AposSolicitacaoAceita() throws Exception {
        // preparar filas existentes
        PassageRequestQueue fila1 = new PassageRequestQueue(); fila1.setId(1L);
        User motorista1 = new User(); motorista1.setId(6L); fila1.setMotorista(motorista1);
        PassageRequestQueue fila2 = new PassageRequestQueue(); fila2.setId(2L);
        User motorista2 = new User(); motorista2.setId(7L); fila2.setMotorista(motorista2);

        // definir status para evitar NPE quando o serviço inspecionar getStatus().getNome()
        PassageRequestQueueStatus enviadaStatus = new PassageRequestQueueStatus();
        enviadaStatus.setNome("enviada");
        fila1.setStatus(enviadaStatus);
        fila2.setStatus(enviadaStatus);
        List<PassageRequestQueue> filas = List.of(fila1, fila2);

        // preparar fila aceita
        PassageRequestQueue filaAceita = new PassageRequestQueue();
        filaAceita.setId(10L);
        User motorista = new User(); motorista.setId(5L);
        filaAceita.setMotorista(motorista);
        filaAceita.setSolicitacao(solicitation);
        PassageRequestQueueStatus enviada = new PassageRequestQueueStatus(); enviada.setNome("enviada"); filaAceita.setStatus(enviada);

        when(passageRequestQueueRepository.findById(10L)).thenReturn(Optional.of(filaAceita));
        when(passageRequestsRepository.findById(1L)).thenReturn(Optional.of(solicitation));
        when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L)).thenReturn(filas);

        // stub necessário para rejeitar/limpar outras filas
        when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(new PassageRequestQueueStatus()));

        when(queueStatusRepository.findByNome("aceita")).thenReturn(Optional.of(new PassageRequestQueueStatus()));
        when(pipelineStatusRepository.findByNome("aceita")).thenReturn(Optional.of(new PassageRequestsPipelineStatus()));
        when(passageRequestsStatusService.findByNome("aceita")).thenReturn(statusSolicitacaoAceita);

        // execute
        service.handleMotoristaAceita(10L, 1L, motorista.getId());

        // verificar que deleteAll foi chamado para limpar filas
        verify(passageRequestQueueRepository, atLeastOnce()).findBySolicitacaoIdOrderByOrdemFilaAsc(1L);
        verify(passageRequestQueueRepository, times(1)).deleteAll(filas);
    }

    @Test
    @DisplayName("Ao recusar definitivamente solicitação, a fila deve ser limpa")
    void testFilaLimpa_AposSolicitacaoRecusada() throws Exception {
        PassageRequestQueue fila1 = new PassageRequestQueue(); fila1.setId(3L);
        PassageRequestQueue fila2 = new PassageRequestQueue(); fila2.setId(4L);
        List<PassageRequestQueue> filas = List.of(fila1, fila2);

        when(passageRequestsStatusService.findByNome("recusada")).thenReturn(new PassageRequestsStatus());
        when(passageRequestQueueRepository.findBySolicitacaoIdOrderByOrdemFilaAsc(1L)).thenReturn(filas);
        when(queueStatusRepository.findByNome("recusada")).thenReturn(Optional.of(new PassageRequestQueueStatus()));

        // execute
        service.marcarSolicitacaoComoRecusada(solicitation);

        verify(passageRequestQueueRepository, atLeastOnce()).findBySolicitacaoIdOrderByOrdemFilaAsc(1L);
        verify(passageRequestQueueRepository, times(1)).deleteAll(filas);
    }

}

