package com.ceramique.repository;

import com.ceramique.persistent.model.LigneCommande;
import com.ceramique.persistent.model.Commande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Long> {
    
    List<LigneCommande> findByCommande_Id(Long commandeId);
    
    List<LigneCommande> findByProduit_Id(Long produitId);
    
    Optional<LigneCommande> findByCommande_IdAndProduit_Id(Long commandeId, Long produitId);
    
    @Query("SELECT lc FROM LigneCommande lc WHERE lc.commande.pointDeVente.id = :pointDeVenteId AND lc.produit.id = :produitId")
    List<LigneCommande> findByPointDeVenteAndProduit(@Param("pointDeVenteId") Long pointDeVenteId, 
                                                     @Param("produitId") Long produitId);
    
    List<LigneCommande> findByCommande(Commande commande);
}
