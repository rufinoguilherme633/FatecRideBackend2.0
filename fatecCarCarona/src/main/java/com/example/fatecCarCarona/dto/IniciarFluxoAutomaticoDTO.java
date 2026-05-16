package com.example.fatecCarCarona.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para iniciar o fluxo automático de caronas.")
public record IniciarFluxoAutomaticoDTO(
		@Schema(description = "ID da solicitação de carona a ser processada.", example = "1")
		Long solicitacaoId,
		@Schema(description = "Latitude da origem da rota (opcional: se ausente, usa a origem persistida na solicitação).", example = "-23.5505")
		Double latitudeOrigem,
		@Schema(description = "Longitude da origem da rota (opcional: se ausente, usa a origem persistida na solicitação).", example = "-46.6333")
		Double longitudeOrigem,
		@Schema(description = "Latitude do destino da rota (opcional: se ausente, usa o destino persistido na solicitação).", example = "-23.5710")
		Double latitudeDestino,
		@Schema(description = "Longitude do destino da rota (opcional: se ausente, usa o destino persistido na solicitação).", example = "-46.6560")
		Double longitudeDestino
) {}


