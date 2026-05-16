package com.example.fatecCarCarona.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fatecCarCarona.entity.PassageRequestQueueStatus;

public interface PassageRequestQueueStatusRepository extends JpaRepository<PassageRequestQueueStatus, Long> {
	Optional<PassageRequestQueueStatus> findByNome(String nome);
}

