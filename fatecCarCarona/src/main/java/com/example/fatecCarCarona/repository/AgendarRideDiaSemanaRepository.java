package com.example.fatecCarCarona.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.fatecCarCarona.entity.AgendarRideDiaSemana;



public interface AgendarRideDiaSemanaRepository extends JpaRepository<AgendarRideDiaSemana, Long> {
	
	@Query("SELECT p FROM AgendarRideDiaSemana p WHERE p.ativo = true")
	List<AgendarRideDiaSemana> findEveryoneWithActiveDays();
	
	List<AgendarRideDiaSemana> findAllByRideId(Long id);
	
	
	//List<AgendarRideDiaSemana> findByUserId(Long id);
}
