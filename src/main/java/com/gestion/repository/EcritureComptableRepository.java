package com.gestion.repository;

import com.gestion.persistent.model.EcritureComptable;
import com.gestion.persistent.model.JournalComptable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EcritureComptableRepository extends JpaRepository<EcritureComptable, Long> {

    List<EcritureComptable> findByPointDeVenteIdOrderByDateEcritureDescIdDesc(Long pointDeVenteId);

    List<EcritureComptable> findByPointDeVenteIdAndJournalOrderByDateEcritureDesc(Long pointDeVenteId, JournalComptable journal);

    List<EcritureComptable> findByPointDeVenteIdAndDateEcritureBetweenOrderByDateEcritureAsc(
            Long pointDeVenteId, LocalDate dateDebut, LocalDate dateFin);

    Optional<EcritureComptable> findByNumeroPieceAndPointDeVenteId(String numeroPiece, Long pointDeVenteId);

    Optional<EcritureComptable> findByReferencePieceAndPointDeVenteId(String referencePiece, Long pointDeVenteId);

    @Query("SELECT COUNT(e) FROM EcritureComptable e WHERE e.pointDeVenteId = :tenantId AND e.numeroPiece LIKE :prefix%")
    Long countByPrefixAndTenant(@Param("prefix") String prefix, @Param("tenantId") Long tenantId);
}
