package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutConformiteAchat;
import java.math.BigDecimal;

public class LigneRapprochementDTO {
    private Long produitId;
    private String produitReference;
    private String produitNom;

    // Quantités
    private BigDecimal quantiteCommandee = BigDecimal.ZERO;
    private BigDecimal quantiteLivree = BigDecimal.ZERO;
    private BigDecimal quantiteFacturee = BigDecimal.ZERO;
    private BigDecimal ecartQuantite = BigDecimal.ZERO; // Facturée - Livrée

    // Prix Unitaires HT
    private BigDecimal prixUnitaireCommande = BigDecimal.ZERO;
    private BigDecimal prixUnitaireFacture = BigDecimal.ZERO;
    private BigDecimal ecartPrixUnitaire = BigDecimal.ZERO; // Facturé - Commandé

    // Écart financier total sur la ligne
    private BigDecimal ecartFinancierTotal = BigDecimal.ZERO;

    private StatutConformiteAchat statutLigne;
    private String statutLibelle;

    public LigneRapprochementDTO() {}

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public BigDecimal getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(BigDecimal quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public BigDecimal getQuantiteLivree() { return quantiteLivree; }
    public void setQuantiteLivree(BigDecimal quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public BigDecimal getQuantiteFacturee() { return quantiteFacturee; }
    public void setQuantiteFacturee(BigDecimal quantiteFacturee) { this.quantiteFacturee = quantiteFacturee; }

    public BigDecimal getEcartQuantite() { return ecartQuantite; }
    public void setEcartQuantite(BigDecimal ecartQuantite) { this.ecartQuantite = ecartQuantite; }

    public BigDecimal getPrixUnitaireCommande() { return prixUnitaireCommande; }
    public void setPrixUnitaireCommande(BigDecimal prixUnitaireCommande) { this.prixUnitaireCommande = prixUnitaireCommande; }

    public BigDecimal getPrixUnitaireFacture() { return prixUnitaireFacture; }
    public void setPrixUnitaireFacture(BigDecimal prixUnitaireFacture) { this.prixUnitaireFacture = prixUnitaireFacture; }

    public BigDecimal getEcartPrixUnitaire() { return ecartPrixUnitaire; }
    public void setEcartPrixUnitaire(BigDecimal ecartPrixUnitaire) { this.ecartPrixUnitaire = ecartPrixUnitaire; }

    public BigDecimal getEcartFinancierTotal() { return ecartFinancierTotal; }
    public void setEcartFinancierTotal(BigDecimal ecartFinancierTotal) { this.ecartFinancierTotal = ecartFinancierTotal; }

    public StatutConformiteAchat getStatutLigne() { return statutLigne; }
    public void setStatutLigne(StatutConformiteAchat statutLigne) {
        this.statutLigne = statutLigne;
        if (statutLigne != null) this.statutLibelle = statutLigne.getLibelle();
    }

    public String getStatutLibelle() { return statutLibelle; }
    public void setStatutLibelle(String statutLibelle) { this.statutLibelle = statutLibelle; }
}
