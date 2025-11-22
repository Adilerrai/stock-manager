package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.QualiteProduit;

import java.math.BigDecimal;

public class LigneCommandeDTO {
    private Long id;
    private Long commandeId;
    private Long produitId;
    private String produitDescription;
    private String designation;
    private String produitReference;
    private Integer quantiteCommandee;
    private Integer quantiteLivree;
    private BigDecimal prixUnitaire;
    private BigDecimal montantLigne;
    private QualiteProduit qualiteProduit;

    // Constructors
    public LigneCommandeDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitDescription() { return produitDescription; }
    public void setProduitDescription(String produitDescription) { this.produitDescription = produitDescription; }

    public QualiteProduit getQualiteProduit() {
        return qualiteProduit;
    }

    public void setQualiteProduit(QualiteProduit qualiteProduit) {
        this.qualiteProduit = qualiteProduit;
    }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public Integer getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(Integer quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }
    public Integer getQuantiteLivree() { return quantiteLivree; }
    public void setQuantiteLivree(Integer quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }
}
