package com.ceramique.repository;

import com.ceramique.persistent.model.LigneCommandeClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneCommandeClientRepository extends JpaRepository<LigneCommandeClient, Long> {
    
    List<LigneCommandeClient> findByCommandeClient_Id(Long commandeClientId);
    
    List<LigneCommandeClient> findByProduit_Id(Long produitId);
    
    void deleteByCommandeClient_Id(Long commandeClientId);
}