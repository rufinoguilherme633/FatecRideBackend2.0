package com.example.fatecCarCarona.dto;

public record IniciarFluxoAutomaticoDTO(
		Long solicitacaoId,
		Double latitudeOrigem,
		Double longitudeOrigem,
		Double latitudeDestino,
		Double longitudeDestino
) {}

