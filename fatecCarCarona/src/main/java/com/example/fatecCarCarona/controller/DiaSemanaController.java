package com.example.fatecCarCarona.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.dto.DiaSemanaDTO;
import com.example.fatecCarCarona.service.DiaSemanaService;


@RestController
@RequestMapping("/dias-semanas")
public class DiaSemanaController {
	
	@Autowired
	DiaSemanaService diaSemanaService;
	
	@GetMapping("")
	public ResponseEntity<List<DiaSemanaDTO>> pegarDiasSemana() {
		
		List<DiaSemanaDTO> diaSemanaDTOs = diaSemanaService.pegarDiasSemana();
		return ResponseEntity.ok(diaSemanaDTOs);
	}

}
