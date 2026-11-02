package com.gestion.repository;

import com.gestion.persistent.model.LigneCommande;
import com.gestion.persistent.model.Commande;
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
    
    @Query("SELECT lc FROM LigneCommande lc WHERE lc.produit.id = :produitId")
    List<LigneCommande> findByProduit( @Param("produitId") Long produitId);
    
    List<LigneCommande> findByCommande(Commande commande);

    @Query("SELECT lc FROM LigneCommande lc " +
           "JOIN FETCH lc.commande c " +
           "JOIN FETCH lc.produit p " +
           "WHERE c.fournisseur.id = :fournisseurId " +
           "AND c.pointDeVenteId = :tenantId " +
           "AND c.statut <> com.gestion.persistent.enums.StatutCommande.ANNULEE " +
           "ORDER BY c.dateCommande DESC")
    List<LigneCommande> findByFournisseurAndTenant(@Param("fournisseurId") Long fournisseurId, @Param("tenantId") Long tenantId);

    @Query("SELECT lc FROM LigneCommande lc " +
           "JOIN FETCH lc.commande c " +
           "JOIN FETCH lc.produit p " +
           "WHERE c.fournisseur.id = :fournisseurId " +
           "AND lc.produit.id = :produitId " +
           "AND c.pointDeVenteId = :tenantId " +
           "AND c.statut <> com.gestion.persistent.enums.StatutCommande.ANNULEE " +
           "ORDER BY c.dateCommande DESC")
    List<LigneCommande> findByFournisseurAndProduitAndTenant(@Param("fournisseurId") Long fournisseurId, @Param("produitId") Long produitId, @Param("tenantId") Long tenantId);
}

