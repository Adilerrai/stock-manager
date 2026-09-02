package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneFactureDTO {
    private Long id;
    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantite;
    private BigDecimal surfaceM2;
    private BigDecimal prixUnitaireHT;
    private BigDecimal tauxTVA;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal remisePourcentage;
    private BigDecimal montantRemise;
    private BigDecimal montantTotal;

    public LigneFactureDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getSurfaceM2() { return surfaceM2; }
    public void setSurfaceM2(BigDecimal surfaceM2) { this.surfaceM2 = surfaceM2; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getMontantRemise() { return montantRemise; }
    public void setMontantRemise(BigDecimal montantRemise) { this.montantRemise = montantRemise; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
}
