package com.gestion.repository;

import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.model.ChequeEffet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChequeEffetRepository extends JpaRepository<ChequeEffet, Long>, ChequeEffetRepositoryCustom {

    List<ChequeEffet> findByPointDeVenteIdOrderByDateEcheanceAsc(Long pointDeVenteId);

    List<ChequeEffet> findByPointDeVenteIdAndStatutOrderByDateEcheanceAsc(Long pointDeVenteId, StatutEffet statut);

    List<ChequeEffet> findByPointDeVenteIdAndSensOrderByDateEcheanceAsc(Long pointDeVenteId, SensEffet sens);

    List<ChequeEffet> findByPointDeVenteIdAndSensAndStatutOrderByDateEcheanceAsc(
            Long pointDeVenteId, SensEffet sens, StatutEffet statut);

    List<ChequeEffet> findByPointDeVenteIdAndDateEcheanceBetweenOrderByDateEcheanceAsc(
            Long pointDeVenteId, LocalDate debut, LocalDate fin);

    Optional<ChequeEffet> findByNumeroPieceAndPointDeVenteId(String numeroPiece, Long pointDeVenteId);

    @Query("SELECT COALESCE(SUM(c.montant), 0) FROM ChequeEffet c WHERE c.pointDeVenteId = :tenantId " +
           "AND c.sens = :sens AND c.statut = :statut")
    BigDecimal sumMontantBySensAndStatut(
            @Param("tenantId") Long tenantId,
            @Param("sens") SensEffet sens,
            @Param("statut") StatutEffet statut);

    @Query("SELECT c FROM ChequeEffet c WHERE c.pointDeVenteId = :tenantId " +
           "AND c.statut IN ('EN_PORTEFEUILLE', 'REMIS_A_L_ENCAISSEMENT') " +
           "AND c.dateEcheance <= :dateLimite ORDER BY c.dateEcheance ASC")
    List<ChequeEffet> findEcheancesProches(
            @Param("tenantId") Long tenantId,
            @Param("dateLimite") LocalDate dateLimite);
}
