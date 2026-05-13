package com.example.fatecCarCarona.dto;

public record PassageRequestsCreateResponseDTO(
    Long id,
    OriginDTO originDTO,
    DestinationDTO destinationDTO
) {}

