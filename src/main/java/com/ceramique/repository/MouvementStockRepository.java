package com.ceramique.repository;

import com.ceramique.persistent.model.MouvementStock;
import com.ceramique.persistent.enums.TypeMouvement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {
    
    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);

    List<MouvementStock> findByTypeMouvementAndDateMouvementBetween(
        TypeMouvement typeMouvement, LocalDateTime debut, LocalDateTime fin);
    
    @Query("SELECT m FROM MouvementStock m ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findOrderByDateMouvementDesc(@Param("tenantId") Long tenantId);
}