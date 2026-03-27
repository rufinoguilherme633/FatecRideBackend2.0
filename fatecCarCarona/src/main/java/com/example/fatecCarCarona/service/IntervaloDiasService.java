package com.example.fatecCarCarona.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.fatecCarCarona.dto.IntervaloDiasDTO;
import com.example.fatecCarCarona.entity.IntervaloDias;
import com.example.fatecCarCarona.repository.IntervaloDiasRepository;


@Service
public class IntervaloDiasService {

	@Autowired
	IntervaloDiasRepository intervaloDiasRepository;
	
	public List<IntervaloDiasDTO> pegarIntervaloDias() {
		
		List<IntervaloDias> intervalosDias = intervaloDiasRepository.findAll(); 
		List<IntervaloDiasDTO> intervaloDiasDTO = new ArrayList<IntervaloDiasDTO>();
		
		for(IntervaloDias intervaloDias : intervalosDias) {
			
			IntervaloDiasDTO dto = new  IntervaloDiasDTO(
					intervaloDias.getId(),
					intervaloDias.getQuantidade_dias()
			);
			
			intervaloDiasDTO.add(dto);
			
		}
		
		return intervaloDiasDTO;
	}
}
