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


    Optional<Facture> findByNumeroFacture(String numeroFacture);

    List<Facture> findByClientId(Long clientId);

    List<Facture> findByStatut(StatutFacture statut);

    @Query("SELECT f FROM Facture f WHERE "+
           "f.dateFacture BETWEEN :dateDebut AND :dateFin " +
           "ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesByPeriode(@Param("dateDebut") LocalDate dateDebut,
                                         @Param("dateFin") LocalDate dateFin);

    @Query("SELECT f FROM Facture f WHERE " +
           "f.montantRestant > 0 AND f.annulee = false " +
           "ORDER BY f.dateFacture DESC")
    List<Facture> findFacturesImpayees();

    @Query("SELECT f FROM Facture f WHERE " +
           "f.dateEcheance < :date AND f.montantRestant > 0 AND f.annulee = false " +
           "ORDER BY f.dateEcheance ASC")
    List<Facture> findFacturesEchues(@Param("date") LocalDate date);

}

