package com.ceramique.repository;

import com.ceramique.persistent.model.Lot;
import com.ceramique.persistent.enums.QualiteProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {
    
    List<Lot> findByProduitIdAndDepotIdAndActifTrueOrderByDateReception(Long produitId, Long depotId);
    
    List<Lot> findByProduitIdAndDepotIdAndQualiteAndQuantiteDisponibleGreaterThanOrderByDateReception(
        Long produitId, Long depotId, QualiteProduit qualite, BigDecimal quantiteMin);
    
    @Query("SELECT l FROM Lot l WHERE l.dateExpiration <= :date AND l.quantiteDisponible > 0")
    List<Lot> findLotsExpiresOuProchesExpiration(LocalDate date);
    
    List<Lot> findByNumeroLivraisonContaining(String numeroLivraison);
}