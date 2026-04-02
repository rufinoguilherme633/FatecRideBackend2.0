package com.example.fatecCarCarona.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.dto.AgendarRideIntervaloDiasDTO;
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

		User user = userRepository.findById(idLong).orElseThrow(() -> new RuntimeException("usuario não encontrado"));

		Ride ride = rideRepository.findById(agendarRide.ride())
				.orElseThrow(() -> new RuntimeException("Compromisso não encontrado"));

		
		if (!ride.getDriver().getId().equals(user.getId())) {
	        throw new SecurityException("Esta carona não pertence a este motorista.");
	    }
		IntervaloDias intervaloDias = intervaloDiasRepository.findById(agendarRide.intervalo_dias())
				.orElseThrow(() -> new RuntimeException("Intervalo de dia não encontrado"));

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
				.orElseThrow(() -> new RuntimeException("Compromisso não encontrado"));
		if (!agenda.isAtivo()) {
			return;
		}
		agenda.setAtivo(false);
		agendarRideIntervaloDiasRepository.save(agenda);

	}

}
