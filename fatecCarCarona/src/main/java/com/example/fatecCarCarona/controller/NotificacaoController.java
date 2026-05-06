package com.example.fatecCarCarona.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.fatecCarCarona.service.SseNotificationService;
import com.example.fatecCarCarona.service.TokenService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notificacoes")
@Slf4j
@Tag(name = "Notificações SSE", description = "Endpoints para conexão, health check e desconexão do SSE")
public class NotificacaoController {

	@Autowired
	private SseNotificationService sseNotificationService;

	@Autowired
	private TokenService tokenService;

	/**
	 * Endpoint SSE para manter conexão aberta e receber notificações em tempo real
	 * Requer autenticação via JWT
	 *
	 * Exemplo de uso (Frontend):
	 * const eventSource = new EventSource('/notificacoes/stream', {
	 *   headers: { 'Authorization': 'Bearer ' + token }
	 * });
	 *
	 * eventSource.addEventListener('nova_solicitacao', (event) => {
	 *   const solicitacao = JSON.parse(event.data);
	 *   console.log('Nova solicitação recebida:', solicitacao);
	 * });
	 */
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Operation(summary = "Conectar ao SSE", description = "Mantém a conexão aberta para receber notificações em tempo real.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Conexão SSE aberta"),
		@ApiResponse(responseCode = "401", description = "Token inválido ou ausente")
	})
	public SseEmitter stream(
			@Parameter(description = "JWT Bearer token (header) or token query param (for EventSource)", required = true)
			@RequestHeader(value = "Authorization", required = false) String authorizationHeader,
			@RequestParam(value = "token", required = false) String tokenParam) {
		try {
			log.info("Nova conexão SSE recebida");

			// Accept token either in Authorization header or as ?token=... (workaround for EventSource)
			String authHeader = authorizationHeader;
			if ((authHeader == null || authHeader.isBlank()) && tokenParam != null && !tokenParam.isBlank()) {
				authHeader = "Bearer " + tokenParam;
			}
			if (authHeader == null || authHeader.isBlank()) {
				log.warn("Tentativa de conectar SSE sem Authorization header nem token query param");
				throw new IllegalArgumentException("Token inválido ou ausente");
			}

			// Extrair userId do token JWT
			Long userId = tokenService.extractUserIdFromHeader(authHeader);
			log.info("Usuário {} conectando ao SSE", userId);

			// Registrar emitter e retornar (mantém conexão aberta)
			return sseNotificationService.registrar(userId);

		} catch (Exception e) {
			log.error("Erro ao conectar ao SSE: {}", e.getMessage());
			throw new IllegalArgumentException("Token inválido ou ausente", e);
		}
	}

	/**
	 * Endpoint de health check para verificar se está conectado
	 * Retorna true/false indicando se o usuário tem conexão SSE ativa
	 */
	@GetMapping("/conectado")
	@Operation(summary = "Verificar conexão SSE", description = "Retorna se o usuário possui uma conexão SSE ativa.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Status retornado com sucesso"),
		@ApiResponse(responseCode = "401", description = "Token inválido ou ausente")
	})
	public ResponseEntity<Boolean> estaConectado(
			@Parameter(description = "JWT Bearer token", required = true)
			@RequestHeader("Authorization") String authorizationHeader) {
		try {
			Long userId = tokenService.extractUserIdFromHeader(authorizationHeader);
			boolean conectado = sseNotificationService.estaConectado(userId);
			log.info("Verificação de conexão SSE para usuário {}: {}", userId, conectado);
			return ResponseEntity.ok(conectado);
		} catch (Exception e) {
			log.error("Erro ao verificar conexão: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
		}
	}

	/**
	 * Endpoint para desconectar manualmente (opcional)
	 * Limpa a conexão SSE do usuário
	 */
	@PostMapping("/desconectar")
	@Operation(summary = "Desconectar do SSE", description = "Remove manualmente a conexão SSE ativa do usuário.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Usuário desconectado com sucesso"),
		@ApiResponse(responseCode = "401", description = "Token inválido ou ausente")
	})
	public ResponseEntity<String> desconectar(
			@Parameter(description = "JWT Bearer token", required = true)
			@RequestHeader("Authorization") String authorizationHeader) {
		try {
			Long userId = tokenService.extractUserIdFromHeader(authorizationHeader);
			sseNotificationService.desconectar(userId);
			log.info("Usuário {} desconectado do SSE", userId);
			return ResponseEntity.ok("Desconectado com sucesso");
		} catch (Exception e) {
			log.error("Erro ao desconectar: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Erro ao desconectar");
		}
	}
}
