package com.example.fatecCarCarona.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta enviada pelo motorista para aceitar ou recusar uma solicitação.")
public record RespostaMotoristaDTO(
		@Schema(description = "ID da solicitação de carona.", example = "1")
		Long solicitacaoId,
		@Schema(description = "ID da entrada na fila do motorista.", example = "6")
		Long filaId,
		@Schema(description = "ID do motorista autenticado.", example = "2")
		Long motoristaId,
		@Schema(description = "Resposta do motorista: aceita ou recusa.", example = "aceita")
		String resposta // "aceita" ou "recusa"
) {}


