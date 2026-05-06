package com.example.fatecCarCarona.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fatecCarCarona.entity.User;
import com.example.fatecCarCarona.service.PassageRequestAutomaticService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller para processar respostas do motorista às notificações de solicitação
 * Responsável por aceitar ou recusar solicitações de carona automática
 */
@RestController
@RequestMapping("/solicitacao/automatico")
@Slf4j
@Tag(name = "Respostas de Solicitação Automática", description = "Endpoints para motoristas responderem às notificações de solicitação")
public class PassageRequestResponseController {

	@Autowired
	private PassageRequestAutomaticService passageRequestAutomaticService;

	/**
	 * Aceitar uma solicitação de carona
	 *
	 * @param filaId ID da entrada na fila
	 * @param solicitacaoId ID da solicitação
	 * @param authentication Autenticação do usuário logado
	 * @return Resposta com status da operação
	 */
	@PostMapping("/{filaId}/aceitar/{solicitacaoId}")
	@Operation(summary = "Aceitar solicitação", description = "Motorista aceita a solicitação de carona automática")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Solicitação aceita com sucesso"),
			@ApiResponse(responseCode = "400", description = "Parâmetros inválidos ou solicitação não encontrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor")
	})
	public ResponseEntity<Map<String, Object>> aceitar(
			@Parameter(description = "ID da entrada na fila") @PathVariable Long filaId,
			@Parameter(description = "ID da solicitação") @PathVariable Long solicitacaoId,
			Authentication authentication) {
		try {
			if (authentication == null || !authentication.isAuthenticated()) {
				log.warn("Tentativa de aceitar solicitação sem autenticação");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("error", "Usuário não autenticado"));
			}

			// Extrair ID do motorista da autenticação
			User motorista = (User) authentication.getPrincipal();
			Long motoristaId = motorista.getId();

			log.info("Motorista {} aceita solicitação {} (filaId: {})", motoristaId, solicitacaoId, filaId);

			// Processar aceitação
			passageRequestAutomaticService.handleMotoristaAceita(filaId, solicitacaoId, motoristaId);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Solicitação aceita com sucesso");
			response.put("status", "ACEITA");
			response.put("solicitacaoId", solicitacaoId);
			response.put("motoristaId", motoristaId);

			log.info("Solicitação {} aceita pelo motorista {}", solicitacaoId, motoristaId);
			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			log.error("Argumento inválido ao aceitar solicitação: {}", e.getMessage());
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Parâmetros inválidos: " + e.getMessage()));
		} catch (Exception e) {
			log.error("Erro ao aceitar solicitação: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Erro ao processar solicitação: " + e.getMessage()));
		}
	}

	/**
	 * Recusar uma solicitação de carona
	 *
	 * @param filaId ID da entrada na fila
	 * @param solicitacaoId ID da solicitação
	 * @param authentication Autenticação do usuário logado
	 * @return Resposta com status da operação
	 */
	@PostMapping("/{filaId}/recusar/{solicitacaoId}")
	@Operation(summary = "Recusar solicitação", description = "Motorista recusa a solicitação de carona automática")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Solicitação recusada com sucesso"),
			@ApiResponse(responseCode = "400", description = "Parâmetros inválidos ou solicitação não encontrada"),
			@ApiResponse(responseCode = "401", description = "Não autenticado"),
			@ApiResponse(responseCode = "500", description = "Erro interno do servidor")
	})
	public ResponseEntity<Map<String, Object>> recusar(
			@Parameter(description = "ID da entrada na fila") @PathVariable Long filaId,
			@Parameter(description = "ID da solicitação") @PathVariable Long solicitacaoId,
			Authentication authentication) {
		try {
			if (authentication == null || !authentication.isAuthenticated()) {
				log.warn("Tentativa de recusar solicitação sem autenticação");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("error", "Usuário não autenticado"));
			}

			// Extrair ID do motorista da autenticação
			User motorista = (User) authentication.getPrincipal();
			Long motoristaId = motorista.getId();

			log.info("Motorista {} recusa solicitação {} (filaId: {})", motoristaId, solicitacaoId, filaId);

			// Processar recusa
			passageRequestAutomaticService.handleMotoristaRecusa(filaId, solicitacaoId, motoristaId);

			Map<String, Object> response = new HashMap<>();
			response.put("message", "Solicitação recusada");
			response.put("status", "RECUSADA");
			response.put("solicitacaoId", solicitacaoId);
			response.put("motoristaId", motoristaId);

			log.info("Solicitação {} recusada pelo motorista {}", solicitacaoId, motoristaId);
			return ResponseEntity.ok(response);

		} catch (IllegalArgumentException e) {
			log.error("Argumento inválido ao recusar solicitação: {}", e.getMessage());
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Parâmetros inválidos: " + e.getMessage()));
		} catch (Exception e) {
			log.error("Erro ao recusar solicitação: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Erro ao processar solicitação: " + e.getMessage()));
		}
	}
}

