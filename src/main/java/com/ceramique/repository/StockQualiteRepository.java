package com.ceramique.repository;

import com.ceramique.persistent.model.StockQualite;
import com.ceramique.persistent.enums.QualiteProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQualiteRepository extends JpaRepository<StockQualite, Long> {

    @Query("SELECT sq FROM StockQualite sq WHERE sq.stock.produit.pointDeVente.id = :pointDeVenteId AND sq.qualite = :qualite")
    List<StockQualite> findByPointDeVenteIdAndQualite(@Param("pointDeVenteId") Long pointDeVenteId, @Param("qualite") QualiteProduit qualite);

    @Query("SELECT sq FROM StockQualite sq WHERE sq.quantiteDisponible <= sq.seuilAlerte AND sq.stock.produit.pointDeVente.id = :pointDeVenteId")
    List<StockQualite> findStocksEnAlerteByPointDeVente(@Param("pointDeVenteId") Long pointDeVenteId);
}
