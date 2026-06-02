package com.example.fatecCarCarona.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fatecCarCarona.entity.PassageRequestQueue;

public interface PassageRequestQueueRepository extends JpaRepository<PassageRequestQueue, Long> {

	List<PassageRequestQueue> findBySolicitacaoIdOrderByOrdemFilaAsc(Long solicitacaoId);

	Optional<PassageRequestQueue> findBySolicitacaoIdAndStatusNome(Long solicitacaoId, String statusNome);

	List<PassageRequestQueue> findByStatusNomeAndDataEnvioLessThan(String statusNome, LocalDateTime limite);

	// Use a native query with explicit table aliases and qualified column names to
	// avoid ambiguous column errors when Hibernate generates joins.
	@Query(value = """
			SELECT DISTINCT s.id_solicitacao
			FROM solicitacao_fila_motoristas f
			JOIN solicitacoes s ON f.id_solicitacao = s.id_solicitacao
			JOIN status_solicitacao ss ON s.id_status_solicitacao = ss.id_status_solicitacao
			WHERE ss.status_nome IN (:statusNomes)
			  AND COALESCE(f.data_resposta, f.data_envio) < :limite
			""", nativeQuery = true)
	List<Long> findDistinctSolicitacaoIdsForCleanup(@Param("statusNomes") List<String> statusNomes,
												   @Param("limite") LocalDateTime limite);

	Optional<PassageRequestQueue> findFirstBySolicitacaoIdAndStatusNomeOrderByOrdemFilaAsc(Long solicitacaoId, String statusNome);
}
