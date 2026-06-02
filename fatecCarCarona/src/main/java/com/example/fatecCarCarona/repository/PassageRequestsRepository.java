package com.example.fatecCarCarona.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.fatecCarCarona.entity.PassageRequestsStatus;
import com.example.fatecCarCarona.entity.PassageRequests;

public interface PassageRequestsRepository extends JpaRepository<PassageRequests, Long> {
	@Query("SELECT p FROM PassageRequests p WHERE p.passageiro.id = :userId AND p.status.nome = 'concluida'")
	Page<PassageRequests> findPassagerConcluidas(Long userId, PageRequest of);
	
	@Query("SELECT p FROM PassageRequests p WHERE p.passageiro.id = :userId AND p.status.nome IN ('aceita', 'recusada', 'cancelada', 'concluida')")
	Page<PassageRequests> findPassagerFinalizadas(Long userId, PageRequest pageRequest);
	
	@Query("SELECT p FROM PassageRequests p WHERE p.passageiro.id = :userId AND p.status.nome = 'pendente'")
	PassageRequests findByPassagePending(Long userId);

	boolean existsByPassageiroIdAndStatusNome(Long userId, String statusNome);

	boolean existsByCaronaIdAndStatusNome(Long caronaId, String statusNome);

	@Modifying
	@Query(value = "UPDATE solicitacoes SET version = 0 WHERE version IS NULL", nativeQuery = true)
	int normalizeNullVersions();

	@Modifying
	@Query(value = """
			UPDATE solicitacoes pr
			JOIN status_solicitacao s ON s.id_status_solicitacao = pr.id_status_solicitacao
			SET pr.id_status_solicitacao = :novoStatusId
			WHERE pr.id_carona = :caronaId
			  AND s.status_nome = 'aceita'
			""", nativeQuery = true)
	int concluirSolicitacoesAceitasDaCarona(@Param("caronaId") Long caronaId,
								 @Param("novoStatusId") Long novoStatusId);


	@Query("""
		    SELECT p FROM PassageRequests p 
		    JOIN p.carona c 
		    WHERE p.status.nome IN ('pendente', 'aceita', 'aguardando_resposta')
		      AND c.status.nome = 'ativa'
		      AND c.driver.id = :driverId

		""")
		List<PassageRequests> requestsForMyRide(Long driverId);
	
	@Query("SELECT p FROM PassageRequests p WHERE p.carona.id = :caronaId AND p.status.nome = 'aceita'")
	List<PassageRequests> findByCaronaIdAndStatusAceita(Long caronaId);

	@Query("SELECT pr FROM PassageRequests pr WHERE pr.passageiro.id = :userId AND pr.status.id = :pendingStatusId")
	List<PassageRequests> findPendingByPassengerId(@Param("userId") Long userId, @Param("pendingStatusId") Long pendingStatusId);
	
	@Query("SELECT pr FROM PassageRequests pr WHERE pr.passageiro.id = :userId AND pr.status.id IN :statusIds")
	List<PassageRequests> findByPassengerIdAndStatusIdIn(@Param("userId") Long userId, @Param("statusIds") List<Long> statusIds);

}
