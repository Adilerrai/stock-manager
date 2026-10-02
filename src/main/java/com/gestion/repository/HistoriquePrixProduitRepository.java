package com.gestion.repository;

import com.gestion.persistent.model.HistoriquePrixProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoriquePrixProduitRepository extends JpaRepository<HistoriquePrixProduit, Long> {
    List<HistoriquePrixProduit> findByProduitIdAndPointDeVenteIdOrderByDateModificationDesc(Long produitId, Long pointDeVenteId);
    List<HistoriquePrixProduit> findByProduitIdOrderByDateModificationDesc(Long produitId);
}
