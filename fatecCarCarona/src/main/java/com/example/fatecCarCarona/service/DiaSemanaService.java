package com.example.fatecCarCarona.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.fatecCarCarona.dto.DiaSemanaDTO;
import com.example.fatecCarCarona.entity.DiaSemana;
import com.example.fatecCarCarona.repository.DiaSemanaRepository;


@Service
public class DiaSemanaService {

	@Autowired
	DiaSemanaRepository diaSemanaRepository;
	
	public List<DiaSemanaDTO> pegarDiasSemana() {
		
		List<DiaSemana> diasSeamana = diaSemanaRepository.findAll(); 
		List<DiaSemanaDTO> diasSemanaDTOs = new ArrayList<DiaSemanaDTO>();
		
		for(DiaSemana diaSemana : diasSeamana) {
			
			DiaSemanaDTO dto = new  DiaSemanaDTO(
					diaSemana.getId(),
					diaSemana.getNome_dia()
			);
			
			diasSemanaDTOs.add(dto);
			
			
			
		}
		
		return diasSemanaDTOs;
	}
}
