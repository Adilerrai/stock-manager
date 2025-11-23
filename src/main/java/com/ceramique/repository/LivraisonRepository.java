package com.ceramique.repository;

import com.ceramique.persistent.enums.StatutLivraison;
import com.ceramique.persistent.model.Commande;
import com.ceramique.persistent.model.Livraison;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivraisonRepository extends JpaRepository<Livraison, Long>, LivraisonRepositoryCustom {
    
    List<Livraison> findByStatutAndPointDeVente_TenantId(StatutLivraison statut, Long tenantId);
    
    Optional<Livraison> findByIdAndPointDeVente_TenantId(Long id, Long tenantId);
    
    List<Livraison> findByPointDeVente_TenantId(Long tenantId);
    
    List<Livraison> findByCommande_IdAndStatut(Long commandeId, StatutLivraison statut);

    List<Livraison> findByStatut(StatutLivraison statut);
}
