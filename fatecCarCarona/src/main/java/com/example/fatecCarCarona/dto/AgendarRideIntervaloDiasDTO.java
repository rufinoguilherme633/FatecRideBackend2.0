package com.example.fatecCarCarona.dto;

import java.time.LocalDate;
import java.util.List;


import jakarta.validation.constraints.NotNull;
public record AgendarRideIntervaloDiasDTO(
		Long ride,
		@NotNull(message = "data Inicio não pode ser nulo")
		LocalDate dataInicio,
		Long intervalo_dias
) {
	
}
