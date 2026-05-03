package com.example.fatecCarCarona.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.fatecCarCarona.dto.AgendarRideIntervaloDiasDTO;
import com.example.fatecCarCarona.entity.AgendarRideDiaSemana;
import com.example.fatecCarCarona.entity.AgendarRideIntervaloDias;



public interface AgendarRideIntervaloDiasRepository extends JpaRepository<AgendarRideIntervaloDias,Long>{
	@Query("SELECT a FROM AgendarRideIntervaloDias a where a.ativo = true")
	List<AgendarRideIntervaloDias> findEveryoneWithActiveDays();

	//List<AgendarRideDiaSemana> findByRideDriverIdAndAtivoTrue(Long id);
	List<AgendarRideIntervaloDias> findByRideDriverIdAndAtivoTrue(Long id);

}
