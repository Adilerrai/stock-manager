package com.ceramique.repository;

import com.ceramique.persistent.model.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // Liste des stocks par point de vente via produit.pointDeVente
    @Query("SELECT s FROM Stock s WHERE s.produit.pointDeVente.id = :pointDeVenteId")
    List<Stock> findByPointDeVenteId(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT s FROM Stock s LEFT JOIN FETCH s.stocksQualite WHERE s.produit.pointDeVente.id = :pointDeVenteId")
    List<Stock> findByPointDeVenteIdWithQualities(@Param("pointDeVenteId") Long pointDeVenteId);

    @Query("SELECT s FROM Stock s LEFT JOIN FETCH s.stocksQualite WHERE s.produit.id = :produitId")
    Optional<Stock> findByProduitIdWithQualities(@Param("produitId") Long produitId);

    Optional<Stock> findByProduitId(Long produitId);
}
