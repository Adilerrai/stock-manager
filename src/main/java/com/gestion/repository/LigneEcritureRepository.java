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
}
