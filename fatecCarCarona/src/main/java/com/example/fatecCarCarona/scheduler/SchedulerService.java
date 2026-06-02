package com.example.fatecCarCarona.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.entity.AgendarRideDiaSemana;
import com.example.fatecCarCarona.entity.AgendarRideIntervaloDias;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.repository.AgendarRideDiaSemanaRepository;
import com.example.fatecCarCarona.repository.AgendarRideIntervaloDiasRepository;
import com.example.fatecCarCarona.repository.PassageRequestQueueRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.RideStatusRepository;
import com.example.fatecCarCarona.service.PassageRequestAutomaticService;
import com.example.fatecCarCarona.service.RideService;

import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class SchedulerService {
	@Autowired
	AgendarRideIntervaloDiasRepository agendarRideIntervaloDiasRepository;

	@Autowired
	AgendarRideDiaSemanaRepository agendarRideDiaSemanaRepository;

	@Autowired
	RideRepository rideRepository;
	
	@Autowired
	RideStatusRepository rideStatusRepository;

	@Autowired
	PassageRequestQueueRepository passageRequestQueueRepository;

	@Autowired
	PassageRequestAutomaticService passageRequestAutomaticService;

	@Autowired
	RideService rideService;

	@Value("${carona.auto.timeout-motorista-segundos:120}")
	private Integer timeoutMotoristaSegundos;

	@Value("${carona.auto.fila-limpeza-retencao-minutos:5}")
	private Integer filaLimpezaRetencaoMinutos;

	@Value("${carona.auto.carona-grace-period-minutos:30}")
	private Integer caronaGracePeriodMinutos;

	public void criarRideAgendadosComIntervalosDias(List<AgendarRideIntervaloDias> allIntervaloDias) {
		LocalDate dataHoje = LocalDate.now();
		for (AgendarRideIntervaloDias agenda : allIntervaloDias) {

			int diferenca = (int) ChronoUnit.DAYS.between(agenda.getDataInicio(), dataHoje);
			System.out.println("a difernreca " + diferenca);
			System.out.println("quantidade diasc " + agenda.getDia_agendamento().getQuantidade_dias());
			if (diferenca > agenda.getDia_agendamento().getQuantidade_dias()) {
				agenda.setAtivo(false);
				agendarRideIntervaloDiasRepository.save(agenda);
				continue;
			}
			
			Ride ride = new Ride();
			ride.setDriver(agenda.getRide().getDriver());
			ride.setOrigin(agenda.getRide().getOrigin());
			ride.setDestination(agenda.getRide().getDestination());
			ride.setVehicle(agenda.getRide().getVehicle());
			ride.setAvailableSeats(agenda.getRide().getAvailableSeats());
			ride.setStatus(rideStatusRepository.findByNome("ativa")); // ou rideStatusService.gellByName("ativa")
			ride.setDateTime(agenda.getRide().getDateTime()); // LocalDateTime do compromisso
			ride.setData_ride(LocalDate.now());
			
			rideRepository.save(ride);

			System.out.println("quem criou foi o dia");
		}
	}

	public void criarRideAgendadosComDiasSemana(List<AgendarRideDiaSemana> allDiaSemana) {

		for (AgendarRideDiaSemana agenda : allDiaSemana) {
			LocalDate hoje = LocalDate.now();
			DayOfWeek diaSemana = hoje.getDayOfWeek();
			String diaSemanaString = diaSemana.toString();

			System.out.println("heis aqui o dia semana " + diaSemanaString);
			System.out.println(agenda.getDia_semana_agendamento().getNome_dia());

			System.out.println(agenda.getDia_semana_agendamento().getNome_dia().equals(diaSemanaString));
			if (agenda.getDia_semana_agendamento().getNome_dia().equals(diaSemanaString)) {
			    Ride ride = new Ride();
			    ride.setDriver(agenda.getRide().getDriver());
			    ride.setOrigin(agenda.getRide().getOrigin());
			    ride.setDestination(agenda.getRide().getDestination());
			    ride.setVehicle(agenda.getRide().getVehicle());
			    ride.setAvailableSeats(agenda.getRide().getAvailableSeats());
			    ride.setStatus(rideStatusRepository.findByNome("ativa"));
			    ride.setDateTime(agenda.getRide().getDateTime());
			    ride.setData_ride(LocalDate.now());

			    rideRepository.save(ride);
			    System.out.println("Ride criado pelo agendamento semanal");
			}
		}
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void executarAgendador() {

		LocalDate dataHoje = LocalDate.now();

		List<AgendarRideIntervaloDias> allIntervaloDias = agendarRideIntervaloDiasRepository
				.findEveryoneWithActiveDays();

		List<AgendarRideDiaSemana> allDiaSemana = agendarRideDiaSemanaRepository
				.findEveryoneWithActiveDays();

		System.out.println("Executando verificação de compromissos...");
		
		criarRideAgendadosComIntervalosDias(allIntervaloDias);
		criarRideAgendadosComDiasSemana(allDiaSemana);


	}

	@Scheduled(fixedDelayString = "${carona.auto.fila-verificacao-intervalo-ms:60000}")
	public void processarFilaAutomaticaPendente() {
		processarTimeoutsAtrasados();
		limparFilasFinalizadasAntigas();
		reconciliarStatusCaronasExpiradas();
	}

	private void processarTimeoutsAtrasados() {
		LocalDateTime limiteTimeout = LocalDateTime.now().minusSeconds(timeoutMotoristaSegundos);
		var filasAtrasadas = passageRequestQueueRepository
				.findByStatusNomeAndDataEnvioLessThan("enviada", limiteTimeout);

		for (var fila : filasAtrasadas) {
			try {
				passageRequestAutomaticService.handleTimeoutMotorista(fila.getId(), fila.getSolicitacao().getId());
			} catch (Exception e) {
				log.warn("Falha ao processar timeout atrasado da fila {}: {}", fila.getId(), e.getMessage());
			}
		}

		if (!filasAtrasadas.isEmpty()) {
			log.info("Timeout retroativo processado para {} fila(s) antiga(s)", filasAtrasadas.size());
		}
	}

	private void limparFilasFinalizadasAntigas() {
		LocalDateTime limiteLimpeza = LocalDateTime.now().minusMinutes(filaLimpezaRetencaoMinutos);
		List<String> statusFinalizados = List.of("cancelada", "recusada", "aceita", "concluida", "falha_final");

		var solicitacoesParaLimpar = passageRequestQueueRepository
				.findDistinctSolicitacaoIdsForCleanup(statusFinalizados, limiteLimpeza);

		for (Long solicitacaoId : solicitacoesParaLimpar) {
			passageRequestAutomaticService.limparFilaSolicitacao(solicitacaoId, "limpeza_periodica");
		}

		if (!solicitacoesParaLimpar.isEmpty()) {
			log.info("Limpeza periódica removeu filas de {} solicitação(ões) finalizada(s)", solicitacoesParaLimpar.size());
		}
	}

	private void reconciliarStatusCaronasExpiradas() {
		LocalDateTime limiteFimDaCarona = LocalDateTime.now().minusMinutes(caronaGracePeriodMinutos);
		rideService.reconciliarCaronasExpiradas(limiteFimDaCarona);
	}
}
