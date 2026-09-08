package com.gestion.repository;

import com.gestion.persistent.model.StockQualite;
import com.gestion.persistent.enums.QualiteProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockQualiteRepository extends JpaRepository<StockQualite, Long> {

    @Query("SELECT sq FROM StockQualite sq WHERE sq.qualite = :qualite")
    List<StockQualite> findByQualite(@Param("qualite") QualiteProduit qualite);

    @Query("SELECT sq FROM StockQualite sq WHERE sq.qualite = :qualite AND (sq.stock.pointDeVenteId = :tenantId OR sq.produit.pointDeVenteId = :tenantId)")
    List<StockQualite> findByQualiteAndTenant(@Param("qualite") QualiteProduit qualite, @Param("tenantId") Long tenantId);

    @Query("SELECT sq FROM StockQualite sq WHERE sq.quantiteDisponible <= sq.seuilAlerte")
    List<StockQualite> findStocksEnAlerte();

    @Query("SELECT sq FROM StockQualite sq WHERE sq.quantiteDisponible <= sq.seuilAlerte AND (sq.stock.pointDeVenteId = :tenantId OR sq.produit.pointDeVenteId = :tenantId)")
    List<StockQualite> findStocksEnAlerteByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT sq FROM StockQualite sq WHERE sq.quantiteDisponible <= 0")
    List<StockQualite> findStocksEnRupture();

    @Query("SELECT sq FROM StockQualite sq WHERE sq.quantiteDisponible <= 0 AND (sq.stock.pointDeVenteId = :tenantId OR sq.produit.pointDeVenteId = :tenantId)")
    List<StockQualite> findStocksEnRuptureByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(sq) FROM StockQualite sq WHERE sq.quantiteDisponible <= 0")
    Long countEnRupture();

    @Query("SELECT COUNT(sq) FROM StockQualite sq WHERE sq.quantiteDisponible <= 0 AND (sq.stock.pointDeVenteId = :tenantId OR sq.produit.pointDeVenteId = :tenantId)")
    Long countEnRuptureByTenant(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(sq) FROM StockQualite sq WHERE sq.quantiteDisponible > 0 AND sq.quantiteDisponible <= sq.seuilAlerte")
    Long countStockBas();

    @Query("SELECT COUNT(sq) FROM StockQualite sq WHERE sq.quantiteDisponible > 0 AND sq.quantiteDisponible <= sq.seuilAlerte AND (sq.stock.pointDeVenteId = :tenantId OR sq.produit.pointDeVenteId = :tenantId)")
    Long countStockBasByTenant(@Param("tenantId") Long tenantId);
}

