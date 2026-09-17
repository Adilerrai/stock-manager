package com.gestion.repository;

import com.gestion.persistent.enums.StatutRapprochement;
import com.gestion.persistent.model.LigneReleveBancaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LigneReleveBancaireRepository extends JpaRepository<LigneReleveBancaire, Long> {

    List<LigneReleveBancaire> findByReleveBancaireIdOrderByDateOperationAsc(Long releveBancaireId);

    List<LigneReleveBancaire> findByReleveBancaireCompteFinancierIdAndStatutAndPointDeVenteIdOrderByDateOperationAsc(
            Long compteFinancierId, StatutRapprochement statut, Long pointDeVenteId);

    @Query("SELECT l FROM LigneReleveBancaire l WHERE l.releveBancaire.compteFinancier.id = :compteId " +
           "AND l.pointDeVenteId = :pointDeVenteId " +
           "AND l.dateOperation <= :dateArrete " +
           "AND l.statut = 'NON_RAPPROCHE' ORDER BY l.dateOperation ASC")
    List<LigneReleveBancaire> findNonRapprocheesAvantDate(
            @Param("compteId") Long compteId,
            @Param("dateArrete") LocalDate dateArrete,
            @Param("pointDeVenteId") Long pointDeVenteId);

    long countByReleveBancaireIdAndStatut(Long releveBancaireId, StatutRapprochement statut);
}
