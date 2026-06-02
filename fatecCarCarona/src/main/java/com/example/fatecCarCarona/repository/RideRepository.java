package com.example.fatecCarCarona.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fatecCarCarona.entity.CaronaDetalhadaProjection;
import com.example.fatecCarCarona.entity.PassageRequests;
import com.example.fatecCarCarona.entity.Ride;

public interface RideRepository extends JpaRepository<Ride, Long> {
	@Query(value = """
		    SELECT
		        u.nome, u.sobrenome, u.foto, u.telefone,
		        c.data_hora, c.vagas_disponiveis as vagas,
		        status.status_nome,
		        v.modelo,v.marca, v.placa, v.cor, v.ano,
		        o.id_origem,o.logradouro as origem_logradouro, o.numero, o.bairro as origem_bairro, o.cep as origem_cep, o.latitude as origem_latitude , o.longitude as origem_longitude,
		        d.id_destino, d.logradouro as destino_logradouro , d.numero as destino_numero ,  d.bairro as destino_bairro, d.cep as destino_cep, d.latitude as destino_latitude, d.longitude  as destino_longitude
		    FROM usuarios u
		    INNER JOIN caronas c ON u.id_usuario = c.id_motorista
		    INNER JOIN status_carona status ON c.id_status_carona = status.id_status_carona
		    INNER JOIN veiculos v ON u.id_usuario = v.id_usuario
		    INNER JOIN origens o ON c.id_origem = o.id_origem
		    INNER JOIN destinos d ON c.id_destino = d.id_destino
		    WHERE u.id_usuario = :idUsuario AND status.status_nome = 'ativa'
		    """, nativeQuery = true)
	CaronaDetalhadaProjection findByRidesAtivasByUser(@Param("idUsuario") Long id);


	@Query("SELECT r FROM Ride r WHERE r.driver.id = :userId AND r.status.nome = 'ativa'")
	List<Ride> findAtivasByDriverId(@Param("userId") Long userId);


	@Query("SELECT r FROM Ride r WHERE r.status.nome = 'ativa'")
	List<Ride> findAllActiveRides();

	List<Ride> findByStatusNomeAndDateTimeBefore(String statusNome, LocalDateTime limite);


	
	@Query("SELECT r FROM Ride r WHERE r.driver.id = :userId AND r.status.nome = 'concluida'")
	Page<Ride> findConcluidasyDriverId(@Param("userId") Long userId, Pageable pageable);


}
