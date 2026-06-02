package com.example.fatecCarCarona.dto;

import java.time.LocalDate;

public record AgendarRideIntervaloDiasResponseDTO(
        Long id,
        Long rideId,
        String rideOrigem,
        String rideDestino,
        Long driverId,
        String driverName,
        LocalDate dataInicio,
        Long intervaloDiasId,
        String intervaloDiasNome,
        boolean ativo
) {
}
