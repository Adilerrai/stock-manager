package com.gestion.repository;

import com.gestion.persistent.model.LigneVente;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {

    List<LigneVente> findByVenteId(Long venteId);

    @Query("SELECT lv.produit.id, lv.produit.reference, lv.produit.designation, SUM(lv.quantite), SUM(lv.montantTTC) " +
           "FROM LigneVente lv WHERE lv.vente.statut = 'VALIDEE' " +
           "GROUP BY lv.produit.id, lv.produit.reference, lv.produit.designation " +
           "ORDER BY SUM(lv.montantTTC) DESC")
    List<Object[]> findTopProduits(Pageable pageable);

    @Query("SELECT lv.produit.id, lv.produit.reference, lv.produit.designation, " +
           "SUM(lv.montantHT), SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0)), " +
           "SUM(lv.quantite), SUM(COALESCE(lv.remiseMontant, 0)) " +
           "FROM LigneVente lv WHERE lv.vente.statut = 'VALIDEE' " +
           "AND lv.vente.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY lv.produit.id, lv.produit.reference, lv.produit.designation " +
           "ORDER BY (SUM(lv.montantHT) - SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParProduit(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT c.id, c.nom, " +
           "SUM(lv.montantHT), SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lv.remiseMontant, 0)) " +
           "FROM LigneVente lv JOIN lv.produit p JOIN p.categorie c " +
           "WHERE lv.vente.statut = 'VALIDEE' " +
           "AND lv.vente.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY c.id, c.nom " +
           "ORDER BY (SUM(lv.montantHT) - SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParCategorie(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT cl.id, cl.nom, cl.nomComplet, " +
           "SUM(lv.montantHT), SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lv.remiseMontant, 0)) " +
           "FROM LigneVente lv JOIN lv.vente v JOIN v.client cl " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY cl.id, cl.nom, cl.nomComplet " +
           "ORDER BY (SUM(lv.montantHT) - SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParClient(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT SUM(lv.montantHT), SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lv.remiseMontant, 0)) " +
           "FROM LigneVente lv WHERE lv.vente.statut = 'VALIDEE' " +
           "AND lv.vente.dateVente BETWEEN :debut AND :fin")
    List<Object[]> calculerTotauxMargeGlobale(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT SUM(lv.montantHT), SUM(lv.quantite * COALESCE(lv.produit.prixAchatHt, lv.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lv.remiseMontant, 0)) " +
           "FROM LigneVente lv WHERE (lv.vente.vendeur.id = :commercialId OR lv.vente.client.commercial.id = :commercialId) " +
           "AND lv.vente.statut = 'VALIDEE' AND lv.vente.dateVente BETWEEN :debut AND :fin")
    List<Object[]> calculerTotauxMargeByCommercial(@Param("commercialId") Long commercialId,
                                                   @Param("debut") LocalDateTime debut,
                                                   @Param("fin") LocalDateTime fin);
}


