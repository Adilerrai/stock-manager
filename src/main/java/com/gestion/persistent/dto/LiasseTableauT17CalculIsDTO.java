package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LiasseTableauT17CalculIsDTO {

    private String titre = "TABLEAU 17 : CALCUL DE L'IMPÔT SUR LES SOCIÉTÉS (IS) ET COTISATION MINIMALE (CM)";
    private Integer annee;

    // A. Calcul de l'IS
    private BigDecimal baseImposableIs = BigDecimal.ZERO;
    private BigDecimal isTheorique = BigDecimal.ZERO;

    // B. Calcul de la Cotisation Minimale (CM)
    private BigDecimal baseCotisationMinimale = BigDecimal.ZERO;
    private BigDecimal tauxCotisationMinimale = BigDecimal.ZERO;
    private BigDecimal montantCotisationMinimaleCalculee = BigDecimal.ZERO;
    private BigDecimal plancherCotisationMinimale = new BigDecimal("3000.00");
    private BigDecimal cotisationMinimaleRetenue = BigDecimal.ZERO;

    // C. Impôt exigible
    private BigDecimal impotExigible = BigDecimal.ZERO; // Max(IS, CM)
    private String regleAppliquee; // "IS_PROGRESSIF" ou "COTISATION_MINIMALE"

    // D. Rapprochement avec les acomptes
    private BigDecimal totalAcomptesVerses = BigDecimal.ZERO;
    private BigDecimal reliquatAPayer = BigDecimal.ZERO;
    private BigDecimal excedentAcomptes = BigDecimal.ZERO;

    public LiasseTableauT17CalculIsDTO() {}

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getBaseImposableIs() { return baseImposableIs; }
    public void setBaseImposableIs(BigDecimal baseImposableIs) { this.baseImposableIs = baseImposableIs; }

    public BigDecimal getIsTheorique() { return isTheorique; }
    public void setIsTheorique(BigDecimal isTheorique) { this.isTheorique = isTheorique; }

    public BigDecimal getBaseCotisationMinimale() { return baseCotisationMinimale; }
    public void setBaseCotisationMinimale(BigDecimal baseCotisationMinimale) { this.baseCotisationMinimale = baseCotisationMinimale; }

    public BigDecimal getTauxCotisationMinimale() { return tauxCotisationMinimale; }
    public void setTauxCotisationMinimale(BigDecimal tauxCotisationMinimale) { this.tauxCotisationMinimale = tauxCotisationMinimale; }

    public BigDecimal getMontantCotisationMinimaleCalculee() { return montantCotisationMinimaleCalculee; }
    public void setMontantCotisationMinimaleCalculee(BigDecimal montantCotisationMinimaleCalculee) { this.montantCotisationMinimaleCalculee = montantCotisationMinimaleCalculee; }

    public BigDecimal getPlancherCotisationMinimale() { return plancherCotisationMinimale; }
    public void setPlancherCotisationMinimale(BigDecimal plancherCotisationMinimale) { this.plancherCotisationMinimale = plancherCotisationMinimale; }

    public BigDecimal getCotisationMinimaleRetenue() { return cotisationMinimaleRetenue; }
    public void setCotisationMinimaleRetenue(BigDecimal cotisationMinimaleRetenue) { this.cotisationMinimaleRetenue = cotisationMinimaleRetenue; }

    public BigDecimal getImpotExigible() { return impotExigible; }
    public void setImpotExigible(BigDecimal impotExigible) { this.impotExigible = impotExigible; }

    public String getRegleAppliquee() { return regleAppliquee; }
    public void setRegleAppliquee(String regleAppliquee) { this.regleAppliquee = regleAppliquee; }

    public BigDecimal getTotalAcomptesVerses() { return totalAcomptesVerses; }
    public void setTotalAcomptesVerses(BigDecimal totalAcomptesVerses) { this.totalAcomptesVerses = totalAcomptesVerses; }

    public BigDecimal getReliquatAPayer() { return reliquatAPayer; }
    public void setReliquatAPayer(BigDecimal reliquatAPayer) { this.reliquatAPayer = reliquatAPayer; }

    public BigDecimal getExcedentAcomptes() { return excedentAcomptes; }
    public void setExcedentAcomptes(BigDecimal excedentAcomptes) { this.excedentAcomptes = excedentAcomptes; }
}
