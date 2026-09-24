package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneDocumentOcrDTO {

    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantite = BigDecimal.ONE;
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;
    private BigDecimal tauxTVA = BigDecimal.valueOf(20.0);
    private BigDecimal montantHT = BigDecimal.ZERO;
    private BigDecimal montantTTC = BigDecimal.ZERO;
    private boolean produitMatched;

    public LigneDocumentOcrDTO() {}

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaireHT() {
        return prixUnitaireHT;
    }

    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) {
        this.prixUnitaireHT = prixUnitaireHT;
    }

    public BigDecimal getTauxTVA() {
        return tauxTVA;
    }

    public void setTauxTVA(BigDecimal tauxTVA) {
        this.tauxTVA = tauxTVA;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public boolean isProduitMatched() {
        return produitMatched;
    }

    public void setProduitMatched(boolean produitMatched) {
        this.produitMatched = produitMatched;
    }
}
