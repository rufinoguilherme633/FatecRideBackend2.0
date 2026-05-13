package com.example.fatecCarCarona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fatecCarCarona.entity.PassageRequestsPipelineStatus;

public interface PassageRequestsPipelineStatusRepository extends JpaRepository<PassageRequestsPipelineStatus, Long> {
	Optional<PassageRequestsPipelineStatus> findByNome(String nome);
}
