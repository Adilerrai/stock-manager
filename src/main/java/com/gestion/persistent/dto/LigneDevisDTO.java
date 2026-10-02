package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneDevisDTO {
    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitDesignation;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireHT;
    private BigDecimal tauxTVA;
    private BigDecimal tauxRemise;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private String description;

    public LigneDevisDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitDesignation() { return produitDesignation; }
    public void setProduitDesignation(String produitDesignation) { this.produitDesignation = produitDesignation; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getTauxRemise() { return tauxRemise; }
    public void setTauxRemise(BigDecimal tauxRemise) { this.tauxRemise = tauxRemise; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Aliases pour compatibilité frontend et Jackson
    public String getDesignation() {
        return produitDesignation != null ? produitDesignation : description;
    }
    public void setDesignation(String designation) {
        if (this.produitDesignation == null) {
            this.produitDesignation = designation;
        }
        if (this.description == null) {
            this.description = designation;
        }
    }

    public String getReference() {
        return produitReference;
    }
    public void setReference(String reference) {
        this.produitReference = reference;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaireHT;
    }
    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaireHT = prixUnitaire;
    }

    public BigDecimal getTauxTva() {
        return tauxTVA;
    }
    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTVA = tauxTva;
    }

    public BigDecimal getRemisePct() {
        return tauxRemise;
    }
    public void setRemisePct(BigDecimal remisePct) {
        this.tauxRemise = remisePct;
    }

    public BigDecimal getMontantTotal() {
        return montantTTC;
    }
    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTTC = montantTotal;
    }
}
