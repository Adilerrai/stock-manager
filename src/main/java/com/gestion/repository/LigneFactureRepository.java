package com.gestion.repository;

import com.gestion.persistent.model.LigneFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {

    List<LigneFacture> findByFactureId(Long factureId);

    @Query("SELECT SUM(lf.montantHT), " +
           "SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lf.remiseMontant, 0)) " +
           "FROM LigneFacture lf WHERE lf.facture.annulee = false " +
           "AND lf.facture.statut NOT IN (com.gestion.persistent.enums.StatutFacture.ANNULEE, com.gestion.persistent.enums.StatutFacture.BROUILLON) " +
           "AND lf.facture.vente IS NULL " +
           "AND lf.facture.dateFacture BETWEEN :debut AND :fin")
    List<Object[]> calculerTotauxMargeGlobale(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT lf.produit.id, COALESCE(lf.produit.reference, lf.reference), COALESCE(lf.produit.designation, lf.designation), " +
           "SUM(lf.montantHT), SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0)), " +
           "SUM(lf.quantite), SUM(COALESCE(lf.remiseMontant, 0)) " +
           "FROM LigneFacture lf WHERE lf.facture.annulee = false " +
           "AND lf.facture.statut NOT IN (com.gestion.persistent.enums.StatutFacture.ANNULEE, com.gestion.persistent.enums.StatutFacture.BROUILLON) " +
           "AND lf.facture.vente IS NULL " +
           "AND lf.facture.dateFacture BETWEEN :debut AND :fin " +
           "GROUP BY lf.produit.id, COALESCE(lf.produit.reference, lf.reference), COALESCE(lf.produit.designation, lf.designation) " +
           "ORDER BY (SUM(lf.montantHT) - SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParProduit(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT c.id, c.nom, " +
           "SUM(lf.montantHT), SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lf.remiseMontant, 0)) " +
           "FROM LigneFacture lf JOIN lf.produit p LEFT JOIN p.categorie c " +
           "WHERE lf.facture.annulee = false " +
           "AND lf.facture.statut NOT IN (com.gestion.persistent.enums.StatutFacture.ANNULEE, com.gestion.persistent.enums.StatutFacture.BROUILLON) " +
           "AND lf.facture.vente IS NULL " +
           "AND lf.facture.dateFacture BETWEEN :debut AND :fin " +
           "GROUP BY c.id, c.nom " +
           "ORDER BY (SUM(lf.montantHT) - SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParCategorie(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);

    @Query("SELECT cl.id, cl.nom, cl.nomComplet, " +
           "SUM(lf.montantHT), SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0)), " +
           "SUM(COALESCE(lf.remiseMontant, 0)) " +
           "FROM LigneFacture lf JOIN lf.facture f JOIN f.client cl " +
           "WHERE f.annulee = false " +
           "AND f.statut NOT IN (com.gestion.persistent.enums.StatutFacture.ANNULEE, com.gestion.persistent.enums.StatutFacture.BROUILLON) " +
           "AND f.vente IS NULL " +
           "AND f.dateFacture BETWEEN :debut AND :fin " +
           "GROUP BY cl.id, cl.nom, cl.nomComplet " +
           "ORDER BY (SUM(lf.montantHT) - SUM(lf.quantite * COALESCE(lf.produit.prixAchatHt, lf.produit.prixAchat, 0))) DESC")
    List<Object[]> calculerMargeParClient(@Param("debut") LocalDate debut, @Param("fin") LocalDate fin);
}
