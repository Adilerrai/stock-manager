package com.gestion.repository;

import com.gestion.persistent.model.MouvementStock;
import com.gestion.persistent.enums.TypeMouvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long>, MouvementStockRepositoryCustom {
    
    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);

    @Query("SELECT m FROM MouvementStock m WHERE m.produit.id = :produitId AND (m.pointDeVenteId = :tenantId OR (m.pointDeVenteId IS NULL AND m.produit.pointDeVenteId = :tenantId)) ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByProduitIdAndTenantIdOrderByDateMouvementDesc(@Param("produitId") Long produitId, @Param("tenantId") Long tenantId);

    List<MouvementStock> findByTypeMouvementAndDateMouvementBetween(
        TypeMouvement typeMouvement, LocalDateTime debut, LocalDateTime fin);

    @Query("SELECT m FROM MouvementStock m WHERE m.typeMouvement = :typeMouvement AND m.dateMouvement BETWEEN :debut AND :fin AND (m.pointDeVenteId = :tenantId OR (m.pointDeVenteId IS NULL AND m.produit.pointDeVenteId = :tenantId)) ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findByTypeMouvementAndDateMouvementBetweenAndTenant(
        @Param("typeMouvement") TypeMouvement typeMouvement, 
        @Param("debut") LocalDateTime debut, 
        @Param("fin") LocalDateTime fin,
        @Param("tenantId") Long tenantId);
    
    @Query("SELECT m FROM MouvementStock m WHERE (m.pointDeVenteId = :tenantId OR (m.pointDeVenteId IS NULL AND m.produit.pointDeVenteId = :tenantId)) ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findOrderByDateMouvementDesc(@Param("tenantId") Long tenantId);

    @Query("SELECT m.produit.id, COALESCE(SUM(m.quantite), 0) FROM MouvementStock m " +
           "WHERE m.typeMouvement IN (com.gestion.persistent.enums.TypeMouvement.SORTIE_VENTE, " +
           "com.gestion.persistent.enums.TypeMouvement.SORTIE_COMMANDE, " +
           "com.gestion.persistent.enums.TypeMouvement.TRANSFERT_SORTIE, " +
           "com.gestion.persistent.enums.TypeMouvement.AJUSTEMENT_NEGATIF) " +
           "AND m.dateMouvement >= :dateDebut " +
           "AND (:tenantId IS NULL OR m.pointDeVenteId = :tenantId OR (m.pointDeVenteId IS NULL AND m.produit.pointDeVenteId = :tenantId)) " +
           "GROUP BY m.produit.id")
    List<Object[]> findSortiesQuantitesParProduitDepuis(@Param("dateDebut") LocalDateTime dateDebut, @Param("tenantId") Long tenantId);
}
