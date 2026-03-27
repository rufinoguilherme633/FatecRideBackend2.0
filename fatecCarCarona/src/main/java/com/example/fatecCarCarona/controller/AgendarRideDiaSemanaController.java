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
import com.example.fatecCarCarona.dto.ListaDiaSemanas;
import com.example.fatecCarCarona.service.AgendarRideDiaSemanaService;
import com.example.fatecCarCarona.service.TokenService;



@RestController
@RequestMapping("/agendar-ride-dia-semana")
public class AgendarRideDiaSemanaController {
	@Autowired
	AgendarRideDiaSemanaService agendarRideDiaSemanaService;
	
	
	 @Autowired
	  private TokenService tokenService;
	 
	 
	@PostMapping
	public ResponseEntity<AgendarRideDiaSemanaDTO> criar(@RequestHeader("Authorization") String authHeader , @RequestBody AgendarRideDiaSemanaDTO agendarRide) {
		Long idLong = tokenService.extractUserIdFromHeader(authHeader);
		AgendarRideDiaSemanaDTO results = agendarRideDiaSemanaService.criarNaAgendaRide(idLong, agendarRide);
		return ResponseEntity.ok(results);
	}
	
	
	
	@PutMapping("/desativar/{id}")
	public ResponseEntity<Void> desativar(@PathVariable Long id, @RequestBody ListaDiaSemanas diasSemana) {
		agendarRideDiaSemanaService.desativar(id,diasSemana);
		return ResponseEntity.noContent().build();
	}
	
	
	
	//fazer
	@GetMapping("")
	public ResponseEntity<List<AgendarRideDiaSemanaDTO>> pegarTodos(@PathVariable Integer id) {
		List<AgendarRideDiaSemanaDTO> todos=  agendarRideDiaSemanaService.pegarTodos(id);
		return ResponseEntity.ok(todos);
	}
	
	
	

}
