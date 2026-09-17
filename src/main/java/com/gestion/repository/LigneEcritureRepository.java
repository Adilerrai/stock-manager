package com.gestion.repository;

import com.gestion.persistent.model.CompteComptable;
import com.gestion.persistent.model.LigneEcriture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LigneEcritureRepository extends JpaRepository<LigneEcriture, Long> {

    @Query("SELECT l FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId AND l.compte = :compte " +
           "AND l.ecriture.dateEcriture BETWEEN :debut AND :fin ORDER BY l.ecriture.dateEcriture ASC, l.id ASC")
    List<LigneEcriture> findLignesPourGrandLivre(
            @Param("tenantId") Long tenantId,
            @Param("compte") CompteComptable compte,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    @Query("SELECT l FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId " +
           "AND l.ecriture.dateEcriture BETWEEN :debut AND :fin")
    List<LigneEcriture> findAllByTenantAndPeriode(
            @Param("tenantId") Long tenantId,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    @Query("SELECT l.ecriture.journal.id, COUNT(DISTINCT l.ecriture.id), COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0), MAX(l.ecriture.dateEcriture) " +
           "FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId AND l.ecriture.journal IS NOT NULL GROUP BY l.ecriture.journal.id")
    List<Object[]> getStatsGroupesParJournal(@Param("tenantId") Long tenantId);

    @Query("SELECT l FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId " +
           "AND l.compte.numeroCompte LIKE CONCAT(:prefixCompte, '%') " +
           "AND (l.lettrage IS NULL OR l.lettrage = '') " +
           "AND l.ecriture.dateEcriture BETWEEN :debut AND :fin " +
           "ORDER BY l.ecriture.dateEcriture ASC, l.id ASC")
    List<LigneEcriture> findLignesNonLettrees(
            @Param("tenantId") Long tenantId,
            @Param("prefixCompte") String prefixCompte,
            @Param("debut") LocalDate debut,
            @Param("fin") LocalDate fin);

    @Query("SELECT l FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId " +
           "AND l.compte.numeroCompte LIKE CONCAT(:prefixCompte, '%') " +
           "AND l.lettrage IS NOT NULL AND l.lettrage != '' " +
           "ORDER BY l.lettrage DESC, l.ecriture.dateEcriture DESC")
    List<LigneEcriture> findLignesLettrees(
            @Param("tenantId") Long tenantId,
            @Param("prefixCompte") String prefixCompte);

    List<LigneEcriture> findByLettrageAndPointDeVenteId(String lettrage, Long tenantId);

    @Query("SELECT MAX(l.lettrage) FROM LigneEcriture l WHERE l.pointDeVenteId = :tenantId AND l.lettrage IS NOT NULL")
    String findDernierLettrage(@Param("tenantId") Long tenantId);
}
