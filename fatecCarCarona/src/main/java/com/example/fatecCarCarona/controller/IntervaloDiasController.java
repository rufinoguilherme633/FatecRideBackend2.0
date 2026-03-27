package com.example.fatecCarCarona.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.dto.IntervaloDiasDTO;
import com.example.fatecCarCarona.service.IntervaloDiasService;



@RestController
@RequestMapping("/intervalos-dias")
public class IntervaloDiasController {

	@Autowired
	IntervaloDiasService intervaloDiasService;
	
	@GetMapping("")
	public ResponseEntity<List<IntervaloDiasDTO>> pegarDiasSemana() {
		
		List<IntervaloDiasDTO> intervaloDiasDTOs = intervaloDiasService.pegarIntervaloDias();
		return ResponseEntity.ok(intervaloDiasDTOs);
	}
	
	
}
