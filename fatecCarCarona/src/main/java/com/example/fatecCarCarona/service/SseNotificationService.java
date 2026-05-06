package com.example.fatecCarCarona.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseNotificationService {

	/**
	 * Armazena os emitters (conexões SSE) por usuário
	 */
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

	/**
	 * Scheduler GLOBAL (boa prática)
	 * Evita criar uma thread por usuário (problema de escala)
	 */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

	/**
	 * Registra uma nova conexão SSE para o usuário
	 */
	public SseEmitter registrar(Long userId) {

		// 🔥 EVITA múltiplas conexões para o mesmo usuário
		if (emitters.containsKey(userId)) {
			log.warn("Usuário {} já possui conexão ativa. Reutilizando conexão existente.", userId);
			return emitters.get(userId);
		}

		// Tempo 0 = sem timeout (ideal para SSE)
		SseEmitter emitter = new SseEmitter(0L);

		try {
			// Evento inicial para confirmar conexão
			emitter.send(SseEmitter.event()
					.id(String.valueOf(userId))
					.name("conexao_estabelecida")
					.data("Conexão SSE estabelecida com sucesso")
					.reconnectTime(5000));

			emitters.put(userId, emitter);
			log.info("Emitter registrado para usuário: {}", userId);

			// Inicia keepalive
			iniciarKeepalive(userId, emitter);

			/**
			 * CALLBACKS - limpeza automática
			 */

			emitter.onCompletion(() -> {
				log.info("Conexão SSE finalizada para usuário: {}", userId);
				limparConexao(userId);
			});

			emitter.onTimeout(() -> {
				log.warn("Timeout na conexão SSE para usuário: {}", userId);
				limparConexao(userId);
			});

			emitter.onError((throwable) -> {
				log.error("Erro na conexão SSE para usuário {}: {}", userId, throwable.getMessage());
				limparConexao(userId);
			});

		} catch (IOException e) {
			log.error("Erro ao registrar emitter para usuário {}: {}", userId, e.getMessage());
			limparConexao(userId);
		}

		return emitter;
	}

	/**
	 * Envia notificação para um usuário específico
	 */
	public void notificar(Long userId, String eventName, Object data) {
		SseEmitter emitter = emitters.get(userId);

		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event()
						.id(String.valueOf(System.currentTimeMillis()))
						.name(eventName)
						.data(data)
						.reconnectTime(5000));

				log.info("Notificação enviada para usuário {}: {}", userId, eventName);

			} catch (IOException e) {
				log.error("Erro ao enviar notificação para usuário {}: {}", userId, e.getMessage());
				limparConexao(userId);
			}
		} else {
			log.warn("Nenhuma conexão SSE ativa para usuário: {}", userId);
		}
	}

	/**
	 * Keepalive para manter conexão ativa
	 * Envia um "comentário" a cada 30 segundos
	 */
	private void iniciarKeepalive(Long userId, SseEmitter emitter) {

		scheduler.scheduleAtFixedRate(() -> {
			try {
				emitter.send(SseEmitter.event()
						.comment("keepalive"));

				log.debug("Keepalive enviado para usuário: {}", userId);

			} catch (IOException e) {
				log.debug("Erro no keepalive para usuário {}: {}", userId, e.getMessage());
				limparConexao(userId);
			}
		}, 30, 30, TimeUnit.SECONDS);
	}

	/**
	 * Limpa conexão do usuário (centraliza lógica)
	 */
	private void limparConexao(Long userId) {
		SseEmitter emitter = emitters.remove(userId);

		if (emitter != null) {
			try {
				emitter.complete();
			} catch (Exception e) {
				log.debug("Erro ao finalizar emitter do usuário {}: {}", userId, e.getMessage());
			}
		}
	}

	/**
	 * Verifica se usuário está conectado
	 */
	public boolean estaConectado(Long userId) {
		return emitters.containsKey(userId);
	}

	/**
	 * Desconecta manualmente um usuário
	 */
	public void desconectar(Long userId) {
		log.info("Desconectando usuário {}", userId);
		limparConexao(userId);
	}

	/**
	 * Retorna número de conexões ativas
	 */
	public int getConexoesAtivas() {
		return emitters.size();
	}
}