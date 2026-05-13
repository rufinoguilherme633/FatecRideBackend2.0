package com.example.fatecCarCarona.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.fatecCarCarona.dto.CourseDTO;
import com.example.fatecCarCarona.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long>{
	
	
	
	//feito para teste de carga
	@Query("SELECT new com.example.fatecCarCarona.dto.CourseDTO(c.id, c.name) FROM Course c")
	List<CourseDTO> findAllDTO();

}
