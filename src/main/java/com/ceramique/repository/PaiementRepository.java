package com.ceramique.repository;

import com.ceramique.persistent.enums.ModePaiement;
import com.ceramique.persistent.model.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    List<Paiement> findByPointDeVenteId(Long pointDeVenteId);

    List<Paiement> findByVenteId(Long venteId);

    List<Paiement> findByFactureId(Long factureId);

    List<Paiement> findByClientId(Long clientId);

    List<Paiement> findByModePaiementAndPointDeVenteId(ModePaiement modePaiement, Long pointDeVenteId);

    @Query("SELECT p FROM Paiement p WHERE p.pointDeVente.id = :pointDeVenteId " +
           "AND p.datePaiement BETWEEN :dateDebut AND :dateFin " +
           "AND p.annule = false " +
           "ORDER BY p.datePaiement DESC")
    List<Paiement> findPaiementsByPeriode(@Param("pointDeVenteId") Long pointDeVenteId,
                                           @Param("dateDebut") LocalDateTime dateDebut,
                                           @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.pointDeVente.id = :pointDeVenteId " +
           "AND p.datePaiement BETWEEN :dateDebut AND :dateFin " +
           "AND p.annule = false")
    BigDecimal sumMontantByPeriode(@Param("pointDeVenteId") Long pointDeVenteId,
                                    @Param("dateDebut") LocalDateTime dateDebut,
                                    @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT SUM(p.montant) FROM Paiement p WHERE p.pointDeVente.id = :pointDeVenteId " +
           "AND p.modePaiement = :modePaiement " +
           "AND p.datePaiement BETWEEN :dateDebut AND :dateFin " +
           "AND p.annule = false")
    BigDecimal sumMontantByModePaiement(@Param("pointDeVenteId") Long pointDeVenteId,
                                         @Param("modePaiement") ModePaiement modePaiement,
                                         @Param("dateDebut") LocalDateTime dateDebut,
                                         @Param("dateFin") LocalDateTime dateFin);
}

