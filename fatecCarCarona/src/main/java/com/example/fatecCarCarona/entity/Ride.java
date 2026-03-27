package com.example.fatecCarCarona.entity;

import java.time.LocalDate;
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
@Table(name = "caronas")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Ride {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carona")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_motorista", nullable = false)
    private User driver;

    @ManyToOne
    @JoinColumn(name = "id_origem", nullable = false)
    private Origin origin;

    @ManyToOne
    @JoinColumn(name = "id_destino", nullable = false)
    private Destination destination;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dateTime;

    @Column(name = "vagas_disponiveis", nullable = false)
    private int availableSeats;

    @ManyToOne
    @JoinColumn(name = "id_status_carona", nullable = false)
    private RideStatus status;

    @ManyToOne
    @JoinColumn(name = "id_veiculo", nullable = false)
    private Vehicle vehicle;
    
    
    //teste
    private LocalDate data_ride;
}
