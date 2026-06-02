package com.example.fatecCarCarona.dto;

import java.time.LocalDateTime;

public record RideDTO(
		OriginDTO originDTO,
		DestinationDTO destinationDTO,
		Integer vagas_disponiveis,
		Long id_veiculo,
		LocalDateTime data_hora_viagem
) {



}
