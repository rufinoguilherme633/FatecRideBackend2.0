package com.example.fatecCarCarona.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitacao_fila_motoristas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PassageRequestQueue {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_fila")
	private Long id;

	@ManyToOne
	@JoinColumn(name = "id_solicitacao", nullable = false)
	private PassageRequests solicitacao;

	@ManyToOne
	@JoinColumn(name = "id_motorista", nullable = false)
	private User motorista;

	@ManyToOne
	@JoinColumn(name = "id_carona", nullable = false)
	private Ride ride;

	@Column(name = "ordem_fila", nullable = false)
	private Integer ordemFila;

	@Column(name = "data_envio")
	private LocalDateTime dataEnvio;

	@Column(name = "data_resposta")
	private LocalDateTime dataResposta;

	@ManyToOne
	@JoinColumn(name = "id_status_fila", nullable = false)
	private PassageRequestQueueStatus status;

	@Column(name = "distancia_origem_km")
	private Double distanciaOrigemKm;
}

