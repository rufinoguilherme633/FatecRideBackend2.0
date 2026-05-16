package com.example.fatecCarCarona.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fatecCarCarona.entity.PassageRequestQueue;

public interface PassageRequestQueueRepository extends JpaRepository<PassageRequestQueue, Long> {

	List<PassageRequestQueue> findBySolicitacaoIdOrderByOrdemFilaAsc(Long solicitacaoId);

	Optional<PassageRequestQueue> findBySolicitacaoIdAndStatusNome(Long solicitacaoId, String statusNome);

	List<PassageRequestQueue> findByStatusNomeAndDataEnvioLessThan(String statusNome, LocalDateTime limite);

	Optional<PassageRequestQueue> findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(Long solicitacaoId, String statusNome);
}

