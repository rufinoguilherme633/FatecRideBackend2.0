package com.example.fatecCarCarona.dto;

public record AgendarRideDiaSemanaResponseDTO(
        Long id, // ID do agendamento específico (AgendarRideDiaSemana)
        Long rideId,
        String rideOrigem,
        String rideDestino,
        Long driverId,
        String driverName,
        Long diaSemanaId,
        String diaSemanaNome,
        boolean ativo
) {
}
