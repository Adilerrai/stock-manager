package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneCoutRevientDTO {

    private Long ligneCommandeId;
    private Long produitId;
    private String reference;
    private String designation;
    private Integer quantite;
    private String devise;
    private BigDecimal prixUnitaireDevise;
    private BigDecimal montantTotalDevise;
    private BigDecimal prixUnitaireMad;
    private BigDecimal montantTotalMad;
    private BigDecimal poidsTotalKg;
    private BigDecimal volumeTotalM3;
    private String codeSh;
    private BigDecimal tauxDroitDouane;
    private BigDecimal quotePartFretMad = BigDecimal.ZERO;
    private BigDecimal quotePartDouaneMad = BigDecimal.ZERO;
    private BigDecimal quotePartChargesLocalesMad = BigDecimal.ZERO;
    private BigDecimal totalFraisAllouesMad = BigDecimal.ZERO;
    private BigDecimal fraisParUniteMad = BigDecimal.ZERO;
    private BigDecimal coutRevientUnitaireMad = BigDecimal.ZERO;
    private BigDecimal coutRevientTotalMad = BigDecimal.ZERO;
    private BigDecimal coefficientLigne = BigDecimal.ONE;

    public LigneCoutRevientDTO() {}

    public Long getLigneCommandeId() { return ligneCommandeId; }
    public void setLigneCommandeId(Long ligneCommandeId) { this.ligneCommandeId = ligneCommandeId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public BigDecimal getPrixUnitaireDevise() { return prixUnitaireDevise; }
    public void setPrixUnitaireDevise(BigDecimal prixUnitaireDevise) { this.prixUnitaireDevise = prixUnitaireDevise; }

    public BigDecimal getMontantTotalDevise() { return montantTotalDevise; }
    public void setMontantTotalDevise(BigDecimal montantTotalDevise) { this.montantTotalDevise = montantTotalDevise; }

    public BigDecimal getPrixUnitaireMad() { return prixUnitaireMad; }
    public void setPrixUnitaireMad(BigDecimal prixUnitaireMad) { this.prixUnitaireMad = prixUnitaireMad; }

    public BigDecimal getMontantTotalMad() { return montantTotalMad; }
    public void setMontantTotalMad(BigDecimal montantTotalMad) { this.montantTotalMad = montantTotalMad; }

    public BigDecimal getPoidsTotalKg() { return poidsTotalKg; }
    public void setPoidsTotalKg(BigDecimal poidsTotalKg) { this.poidsTotalKg = poidsTotalKg; }

    public BigDecimal getVolumeTotalM3() { return volumeTotalM3; }
    public void setVolumeTotalM3(BigDecimal volumeTotalM3) { this.volumeTotalM3 = volumeTotalM3; }

    public String getCodeSh() { return codeSh; }
    public void setCodeSh(String codeSh) { this.codeSh = codeSh; }

    public BigDecimal getTauxDroitDouane() { return tauxDroitDouane; }
    public void setTauxDroitDouane(BigDecimal tauxDroitDouane) { this.tauxDroitDouane = tauxDroitDouane; }

    public BigDecimal getQuotePartFretMad() { return quotePartFretMad; }
    public void setQuotePartFretMad(BigDecimal quotePartFretMad) { this.quotePartFretMad = quotePartFretMad; }

    public BigDecimal getQuotePartDouaneMad() { return quotePartDouaneMad; }
    public void setQuotePartDouaneMad(BigDecimal quotePartDouaneMad) { this.quotePartDouaneMad = quotePartDouaneMad; }

    public BigDecimal getQuotePartChargesLocalesMad() { return quotePartChargesLocalesMad; }
    public void setQuotePartChargesLocalesMad(BigDecimal quotePartChargesLocalesMad) { this.quotePartChargesLocalesMad = quotePartChargesLocalesMad; }

    public BigDecimal getTotalFraisAllouesMad() { return totalFraisAllouesMad; }
    public void setTotalFraisAllouesMad(BigDecimal totalFraisAllouesMad) { this.totalFraisAllouesMad = totalFraisAllouesMad; }

    public BigDecimal getFraisParUniteMad() { return fraisParUniteMad; }
    public void setFraisParUniteMad(BigDecimal fraisParUniteMad) { this.fraisParUniteMad = fraisParUniteMad; }

    public BigDecimal getCoutRevientUnitaireMad() { return coutRevientUnitaireMad; }
    public void setCoutRevientUnitaireMad(BigDecimal coutRevientUnitaireMad) { this.coutRevientUnitaireMad = coutRevientUnitaireMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }

    public BigDecimal getCoefficientLigne() { return coefficientLigne; }
    public void setCoefficientLigne(BigDecimal coefficientLigne) { this.coefficientLigne = coefficientLigne; }
}
