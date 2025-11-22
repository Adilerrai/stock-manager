package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutFacture;
import com.ceramique.persistent.model.Facture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {

    List<Facture> findByPointDeVenteId(Long pointDeVenteId);

    Optional<Facture> findByNumeroFactureAndPointDeVenteId(String numeroFacture, Long pointDeVenteId);

    List<Facture> findByClientIdAndPointDeVenteId(Long clientId, Long pointDeVenteId);

    List<Facture> findByStatutAndPointDeVenteId(StatutFacture statut, Long pointDeVenteId);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVente.id = :pointDeVenteId " +
           "AND f.dateFacture BETWEEN :dateDebut AND :dateFin " +
           "ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesByPeriode(@Param("pointDeVenteId") Long pointDeVenteId,
                                         @Param("dateDebut") LocalDate dateDebut,
                                         @Param("dateFin") LocalDate dateFin);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVente.id = :pointDeVenteId " +
           "AND f.montantRestant > 0 AND f.annulee = false " +
           "ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesImpayees(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT f FROM Facture f WHERE f.pointDeVente.id = :pointDeVenteId " +
           "AND f.dateEcheance < :date AND f.montantRestant > 0 AND f.annulee = false " +
           "ORDER BY f.dateEcheance ASC")
    List<Facture> findFacturesEchues(@Param("pointDeVenteId") Long pointDeVenteId, @Param("date") LocalDate date);
}

