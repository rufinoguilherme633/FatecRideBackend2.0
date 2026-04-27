package com.example.fatecCarCarona.service;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseNotificationService {

	private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Long, ScheduledExecutorService> keepaliveExecutors = new ConcurrentHashMap<>();
	private static final long SSE_TIMEOUT = Long.MAX_VALUE; // Sem timeout de conexão
	private static final long KEEPALIVE_INTERVAL_SECONDS = 30; // Enviar keepalive a cada 30s

	/**
	 * Registra um novo emitter para um usuário e inicia keepalive automático
	 */
	public SseEmitter registrar(Long userId) {
		log.info("Registrando novo emitter para usuário: {}", userId);

		// Remover emitter anterior se existir
		if (emitters.containsKey(userId)) {
			try {
				emitters.get(userId).complete();
			} catch (Exception e) {
				log.warn("Erro ao fechar emitter anterior do usuário {}: {}", userId, e.getMessage());
			}
		}

		// Criar novo emitter
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

		// Callbacks para limpeza automática
		emitter.onCompletion(() -> {
			log.info("SSE completado para usuário: {}", userId);
			limparUsuario(userId);
		});

		emitter.onTimeout(() -> {
			log.info("SSE timeout para usuário: {}", userId);
			limparUsuario(userId);
		});

		emitter.onError(e -> {
			log.error("Erro no SSE do usuário {}: {}", userId, e.getMessage());
			limparUsuario(userId);
		});

		emitters.put(userId, emitter);

		// Iniciar keepalive automático
		iniciarKeepalive(userId, emitter);

		return emitter;
	}

	/**
	 * Inicia thread de keepalive para manter conexão viva
	 */
	private void iniciarKeepalive(Long userId, SseEmitter emitter) {
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, r -> {
			Thread t = new Thread(r, "SSE-Keepalive-" + userId);
			t.setDaemon(true);
			return t;
		});

		executor.scheduleAtFixedRate(() -> {
			try {
				emitter.send(SseEmitter.event()
					.id(userId + "-" + System.currentTimeMillis())
					.comment("keepalive")
					.build());
				log.debug("Keepalive enviado para usuário: {}", userId);
			} catch (IOException e) {
				log.error("Erro ao enviar keepalive para usuário {}: {}", userId, e.getMessage());
				limparUsuario(userId);
			}
		}, KEEPALIVE_INTERVAL_SECONDS, KEEPALIVE_INTERVAL_SECONDS, TimeUnit.SECONDS);

		keepaliveExecutors.put(userId, executor);
	}

	/**
	 * Envia notificação para um usuário específico
	 */
	public void notificar(Long userId, String evento, Object payload) {
		SseEmitter emitter = emitters.get(userId);
		if (emitter != null) {
			try {
				log.info("Enviando notificação '{}' para usuário: {}", evento, userId);
				emitter.send(SseEmitter.event()
					.id(userId + "-" + System.currentTimeMillis())
					.name(evento)
					.data(payload)
					.build());
			} catch (IOException e) {
				log.error("Erro ao enviar notificação para usuário {}: {}", userId, e.getMessage());
				limparUsuario(userId);
			}
		} else {
			log.warn("Usuário {} não está conectado ao SSE", userId);
		}
	}

	/**
	 * Verifica se um usuário está conectado
	 */
	public boolean estaConectado(Long userId) {
		return emitters.containsKey(userId);
	}

	/**
	 * Limpa recursos do usuário (emitter e keepalive)
	 */
	private void limparUsuario(Long userId) {
		log.info("Limpando recursos do usuário: {}", userId);

		// Remover emitter
		SseEmitter emitter = emitters.remove(userId);
		if (emitter != null) {
			try {
				emitter.complete();
			} catch (Exception e) {
				log.debug("Emitter já estava fechado para usuário {}", userId);
			}
		}

		// Parar keepalive
		ScheduledExecutorService executor = keepaliveExecutors.remove(userId);
		if (executor != null) {
			executor.shutdown();
			try {
				if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
					executor.shutdownNow();
				}
			} catch (InterruptedException e) {
				executor.shutdownNow();
				log.warn("Interrupção ao parar executor de keepalive para usuário {}", userId);
			}
		}
	}

	/**
	 * Desconecta manualmente um usuário
	 */
	public void desconectar(Long userId) {
		log.info("Desconectando usuário: {}", userId);
		limparUsuario(userId);
	}
}



