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
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitacoes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PassageRequests {
	@Id
	@GeneratedValue(strategy =  GenerationType.IDENTITY)
	@Column(name = "id_solicitacao")
	Long  id;
	
	@ManyToOne	
	@JoinColumn(name = "id_carona")
	Ride carona;
	
	@ManyToOne	
	@JoinColumn(name = "id_passageiro")
	User passageiro;	
	
    @ManyToOne
    @JoinColumn(name = "id_origem", nullable = false)
    private Origin origin;

    @ManyToOne
    @JoinColumn(name = "id_destino", nullable = false)
    private Destination destination;
	
	@Column(name="data_solicitacao")
	LocalDateTime dataHora;

    @ManyToOne
    @JoinColumn(name = "id_status_solicitacao", nullable = false)
	PassageRequestsStatus status;

	@Column(name = "tentativa_atual", nullable = false)
	private Integer tentativaAtual = 0;

	@ManyToOne
	@JoinColumn(name = "id_status_pipeline")
	private PassageRequestsPipelineStatus statusPipeline;

	// Optimistic locking to avoid concurrent acceptance races
	@Version
	@Column(name = "version", nullable = false)
	private long version = 0L;

}
