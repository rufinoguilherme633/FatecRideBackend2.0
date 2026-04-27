package com.example.fatecCarCarona.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.fatecCarCarona.service.SseNotificationService;
import com.example.fatecCarCarona.service.TokenService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/notificacoes")
@Slf4j
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
	public SseEmitter stream(@RequestHeader("Authorization") String authorizationHeader) {
		try {
			log.info("Nova conexão SSE recebida");

			// Extrair userId do token JWT
			Long userId = tokenService.extractUserIdFromHeader(authorizationHeader);
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
	public ResponseEntity<Boolean> estaConectado(@RequestHeader("Authorization") String authorizationHeader) {
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
	public ResponseEntity<String> desconectar(@RequestHeader("Authorization") String authorizationHeader) {
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
