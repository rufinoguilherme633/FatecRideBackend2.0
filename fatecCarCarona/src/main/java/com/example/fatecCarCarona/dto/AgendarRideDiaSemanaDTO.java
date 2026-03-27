package com.example.fatecCarCarona.dto;

import java.time.LocalDate;
import java.util.List;


public record AgendarRideDiaSemanaDTO(

		Long ride,
		
		List<Long> dia_semana_agendamento
) {
	
}
