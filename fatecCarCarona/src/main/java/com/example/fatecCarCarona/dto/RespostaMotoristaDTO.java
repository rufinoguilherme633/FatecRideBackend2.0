package com.example.fatecCarCarona.dto;

public record RespostaMotoristaDTO(
		Long solicitacaoId,
		Long filaId,
		Long motoristaId,
		String resposta // "aceita" ou "recusa"
) {}

