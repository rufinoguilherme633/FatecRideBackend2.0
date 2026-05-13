package com.example.fatecCarCarona.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.fatecCarCarona.dto.AgendarRideIntervaloDiasDTO;
import com.example.fatecCarCarona.entity.AgendarRideDiaSemana;
import com.example.fatecCarCarona.entity.AgendarRideIntervaloDias;
import com.example.fatecCarCarona.entity.IntervaloDias;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.AgendarRideIntervaloDiasRepository;
import com.example.fatecCarCarona.repository.IntervaloDiasRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.UserRepository;

@Service
public class AgendarRideIntervaloDiasService {

	@Autowired
	RideRepository rideRepository;

	@Autowired
	IntervaloDiasRepository intervaloDiasRepository;

	@Autowired
	AgendarRideIntervaloDiasRepository agendarRideIntervaloDiasRepository;

	@Autowired
	UserRepository userRepository;

	public AgendarRideIntervaloDiasDTO criarNaAgendaRide(Long idLong, AgendarRideIntervaloDiasDTO agendarRide) {

		User user = userRepository.findById(idLong).orElseThrow(() -> new ResponseStatusException(
	            HttpStatus.NOT_FOUND, "usuario não encontrado"));

		Ride ride = rideRepository.findById(agendarRide.ride())
				.orElseThrow(() -> new RuntimeException("Compromisso não encontrado"));

		
		if (!ride.getDriver().getId().equals(user.getId())) {
			throw new ResponseStatusException(
		            HttpStatus.FORBIDDEN,
		            "Esta carona não pertence a este motorista."
		        );
	    }
		IntervaloDias intervaloDias = intervaloDiasRepository.findById(agendarRide.intervalo_dias())
				.orElseThrow(() -> new ResponseStatusException(
	            HttpStatus.NOT_FOUND, "Intervalo de dia não encontrado"));
				

		LocalDate dataAtual = LocalDate.now();

		AgendarRideIntervaloDias insert = new AgendarRideIntervaloDias();
		insert.setRide(ride);
		insert.setDia_agendamento(intervaloDias);
		insert.setDataInicio(dataAtual);
		insert.setAtivo(true);
		AgendarRideIntervaloDias criarNaAgenda = agendarRideIntervaloDiasRepository.save(insert);
		return agendarRide;

	}

	public void desativar(Long id) {

		AgendarRideIntervaloDias agenda = agendarRideIntervaloDiasRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(
			            HttpStatus.NOT_FOUND, "Compromisso não encontrado"));
		if (!agenda.isAtivo()) {
			return;
		}
		agenda.setAtivo(false);
		agendarRideIntervaloDiasRepository.save(agenda);

	}

	public List<AgendarRideIntervaloDias> pegarTodos(Long idLong) {
		User user = userRepository.findById(idLong).orElseThrow(() -> new ResponseStatusException(
	            HttpStatus.NOT_FOUND, "usuario não encontrado"));
		
		//List<AgendarRideDiaSemana> minhalista = agendarRideIntervaloDiasRepository.findByRideDriverIdAndAtivoTrue(user.getId());
		List<AgendarRideIntervaloDias> minhalista = agendarRideIntervaloDiasRepository.findByRideDriverIdAndAtivoTrue(user.getId());
		
		
		
		return minhalista;
	}

}
