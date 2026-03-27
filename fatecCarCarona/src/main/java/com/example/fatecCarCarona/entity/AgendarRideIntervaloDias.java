package com.example.fatecCarCarona.entity;

import java.time.LocalDate;

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
@Table(name ="agendamento_ride_intervalo_dias")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AgendarRideIntervaloDias {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
	@ManyToOne
	@JoinColumn(name = "id_ride")
	private Ride ride;
	private LocalDate dataInicio;
	@ManyToOne
	@JoinColumn(name = "id_intervalo_dias")
	private IntervaloDias dia_agendamento;
	
	private boolean ativo;
	

}


