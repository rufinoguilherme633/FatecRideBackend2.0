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

	// Map para armazenar emitters (conexões SSE) por userId
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

	// Map para armazenar schedulers de keepalive por userId
	private final Map<Long, ScheduledExecutorService> keepaliveSchedulers = new ConcurrentHashMap<>();

	/**
	 * Registra um novo emitter (conexão SSE) para um usuário
	 * Configura keepalive automático para evitar timeout
	 */
	public SseEmitter registrar(Long userId) {
		// Remover conexão anterior se existir
		desconectar(userId);

		SseEmitter emitter = new SseEmitter(600000L); // timeout de 10 minutos

		try {
			// Enviar primeiro evento para confirmar conexão
			emitter.send(SseEmitter.event()
					.id(String.valueOf(userId))
					.name("conexao_estabelecida")
					.data("Conexão SSE estabelecida com sucesso")
					.reconnectTime(5000));

			emitters.put(userId, emitter);
			log.info("Emitter registrado para usuário: {}", userId);

			// Iniciar keepalive automático
			iniciarKeepalive(userId, emitter);

			// Callback para limpeza quando a conexão é fechada
			emitter.onCompletion(() -> {
				log.info("Conexão SSE completada para usuário: {}", userId);
				emitters.remove(userId);
				pararKeepalive(userId);
			});

			emitter.onTimeout(() -> {
				log.warn("Timeout na conexão SSE para usuário: {}", userId);
				emitters.remove(userId);
				pararKeepalive(userId);
			});

			emitter.onError(throwable -> {
				log.error("Erro na conexão SSE para usuário {}: {}", userId, throwable.getMessage());
				emitters.remove(userId);
				pararKeepalive(userId);
			});

		} catch (IOException e) {
			log.error("Erro ao registrar emitter para usuário {}: {}", userId, e.getMessage());
			emitters.remove(userId);
		}

		return emitter;
	}

	/**
	 * Envia uma notificação para um usuário específico
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
				emitters.remove(userId);
				pararKeepalive(userId);
			}
		} else {
			log.warn("Nenhuma conexão SSE ativa para usuário: {}", userId);
		}
	}

	/**
	 * Inicia keepalive automático para manter conexão aberta
	 * Envia um comentário a cada 30 segundos
	 */
	private void iniciarKeepalive(Long userId, SseEmitter emitter) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, r -> {
			Thread t = new Thread(r, "SSE-Keepalive-" + userId);
			t.setDaemon(true);
			return t;
		});

		scheduler.scheduleAtFixedRate(() -> {
			try {
				emitter.send(SseEmitter.event()
						.id(String.valueOf(userId + "-keepalive"))
						.comment("keepalive"));
				log.debug("Keepalive enviado para usuário: {}", userId);
			} catch (IOException e) {
				log.debug("Erro ao enviar keepalive para usuário {}: {}", userId, e.getMessage());
				scheduler.shutdown();
				emitters.remove(userId);
				pararKeepalive(userId);
			}
		}, 30, 30, TimeUnit.SECONDS);

		keepaliveSchedulers.put(userId, scheduler);
	}

	/**
	 * Para o keepalive automático para um usuário
	 */
	private void pararKeepalive(Long userId) {
		ScheduledExecutorService scheduler = keepaliveSchedulers.remove(userId);
		if (scheduler != null && !scheduler.isShutdown()) {
			scheduler.shutdown();
			try {
				if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
					scheduler.shutdownNow();
				}
			} catch (InterruptedException e) {
				scheduler.shutdownNow();
			}
		}
	}

	/**
	 * Verifica se um usuário está conectado
	 */
	public boolean estaConectado(Long userId) {
		return emitters.containsKey(userId);
	}

	/**
	 * Desconecta um usuário manualmente
	 */
	public void desconectar(Long userId) {
		SseEmitter emitter = emitters.remove(userId);
		pararKeepalive(userId);

		if (emitter != null) {
			try {
				emitter.complete();
				log.info("Usuário {} desconectado", userId);
			} catch (Exception e) {
				log.debug("Erro ao completar emitter para usuário {}: {}", userId, e.getMessage());
			}
		}
	}

	/**
	 * Retorna o número de conexões ativas
	 */
	public int getConexoesAticas() {
		return emitters.size();
	}
}



