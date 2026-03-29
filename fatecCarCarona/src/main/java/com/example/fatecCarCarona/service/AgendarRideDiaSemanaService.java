package com.example.fatecCarCarona.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.dto.AgendarRideDiaSemanaDTO;
import com.example.fatecCarCarona.dto.ListaDiaSemanas;
import com.example.fatecCarCarona.entity.AgendarRideDiaSemana;
import com.example.fatecCarCarona.entity.DiaSemana;
import com.example.fatecCarCarona.entity.Ride;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.repository.AgendarRideDiaSemanaRepository;
import com.example.fatecCarCarona.repository.DiaSemanaRepository;
import com.example.fatecCarCarona.repository.RideRepository;
import com.example.fatecCarCarona.repository.UserRepository;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.transaction.Transactional;

@Service
public class AgendarRideDiaSemanaService {
	@Autowired
	AgendarRideDiaSemanaRepository agendarRideDiaSemanaRepository;

	@Autowired
	RideRepository rideRepository;

	@Autowired
	DiaSemanaRepository diaSemanaRepository;

	@Autowired
	UserRepository userRepository;

	@Transactional
	public AgendarRideDiaSemanaDTO criarNaAgendaRide(Long idLong, AgendarRideDiaSemanaDTO agendarRide) {

		User user = userRepository.findById(idLong).orElseThrow(() -> new RuntimeException("usuario não encontrado"));

		Ride ride = rideRepository.findById(agendarRide.ride())
				.orElseThrow(() -> new RuntimeException("Ride não encontrado"));

		// 3. Verificar se o usuário é o motorista da carona
	    if (!ride.getDriver().getId().equals(user.getId())) {
	        throw new SecurityException("Esta carona não pertence a este motorista.");
	    }
		System.out.println("antes do for");
		for (Long dia_semana : agendarRide.dia_semana_agendamento()) {

			System.out.println("depois do for" + dia_semana);

			DiaSemana diaSemana = diaSemanaRepository.findById(dia_semana)
					.orElseThrow(() -> new RuntimeException("Dia semana não encontrado  não encontrado"));

			AgendarRideDiaSemana insert = new AgendarRideDiaSemana();
			insert.setRide(ride);
			insert.setDia_semana_agendamento(diaSemana);
			insert.setAtivo(true);
			agendarRideDiaSemanaRepository.save(insert);

		}

		return agendarRide;

	}

	public void desativar(Long id, ListaDiaSemanas diasSemana) {

		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("usuario não encontrado"));

		List<AgendarRideDiaSemana> lista = agendarRideDiaSemanaRepository.findAllByRideId(id);

		for (AgendarRideDiaSemana i : lista) {

			if (diasSemana.diasSemana().contains(i.getDia_semana_agendamento().getId())) {
				i.setAtivo(false);

			}
		}

		agendarRideDiaSemanaRepository.saveAll(lista);

	}
	/*
	public List<AgendarRideDiaSemanaDTO> pegarTodos(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("usuario não encontrado"));
		
		List<AgendarRideDiaSemana> minhalista = agendarRideDiaSemanaRepository.findByUserId(user.getId());
		
		


		List<AgendarRideDiaSemanaDTO> minhaListaDTO = new ArrayList<AgendarRideDiaSemanaDTO>();
		
		
		for(AgendarRideDiaSemana i:minhalista) {
			AgendarRideDiaSemanaDTO dto = new AgendarRideDiaSemanaDTO(
			
					
					);
			
		}
		
				return minhalista;
	}
*/
}
