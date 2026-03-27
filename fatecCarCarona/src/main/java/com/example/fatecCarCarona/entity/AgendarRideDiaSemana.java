package com.example.fatecCarCarona.entity;


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
@Table(name ="agendamento_ride_dia_semana")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendarRideDiaSemana {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
	@ManyToOne
	@JoinColumn(name = "id_ride")
	private Ride ride;

	@ManyToOne
	@JoinColumn(name = "id_dia_semana")
	private DiaSemana dia_semana_agendamento;
	private boolean ativo;
}
