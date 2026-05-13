package com.example.fatecCarCarona.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.dto.AgendarRideDiaSemanaDTO;
import com.example.fatecCarCarona.dto.AgendarRideIntervaloDiasDTO;
import com.example.fatecCarCarona.entity.AgendarRideIntervaloDias;
import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.service.AgendarRideIntervaloDiasService;
import com.example.fatecCarCarona.service.TokenService;

import jakarta.validation.Valid;
@RestController
@RequestMapping("/agendar-compromisso-intervalo-dias")
public class AgendarRideIntervaloDiasController {

	@Autowired
	AgendarRideIntervaloDiasService agendarRideIntervaloDiasService;
	
	 @Autowired
	  private TokenService tokenService;
	 
	@PostMapping
	public ResponseEntity<AgendarRideIntervaloDiasDTO> agendarRide(@RequestHeader("Authorization") String authHeader , @RequestBody   @Valid AgendarRideIntervaloDiasDTO agendarCompromisso) {
		Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		AgendarRideIntervaloDiasDTO results = agendarRideIntervaloDiasService.criarNaAgendaRide(idLong, agendarCompromisso);
		return ResponseEntity.ok(results);
	}
	
	
	
	@PutMapping("/desativar/{id}")
	public ResponseEntity<Void> desativar(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
		Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		agendarRideIntervaloDiasService.desativar(id);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping
	public List<AgendarRideIntervaloDias> pegarTodos(@RequestHeader("Authorization") String authHeader) {
		Long idLong = tokenService.extractUserIdFromHeader(authHeader);

		List<AgendarRideIntervaloDias> todos=  agendarRideIntervaloDiasService.pegarTodos(idLong);
		return todos;
	}
}
