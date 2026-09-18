package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LiasseTableauT14AffectationDTO {

    private String titre = "TABLEAU 14 : AFFECTATION DES RÉSULTATS INTERVENUE AU COURS DE L'EXERCICE";
    private Integer annee;

    // A. Origine
    private BigDecimal reportANouveauAnterieur = BigDecimal.ZERO;
    private BigDecimal resultatNetExercicePrecedent = BigDecimal.ZERO;
    private BigDecimal prelevementsSurReserves = BigDecimal.ZERO;
    private BigDecimal totalOrigine = BigDecimal.ZERO;

    // B. Affectations
    private BigDecimal reserveLegale = BigDecimal.ZERO;
    private BigDecimal autresReserves = BigDecimal.ZERO;
    private BigDecimal dividendesDistribues = BigDecimal.ZERO;
    private BigDecimal autresAffectations = BigDecimal.ZERO;
    private BigDecimal reportANouveauSolde = BigDecimal.ZERO;
    private BigDecimal totalAffectations = BigDecimal.ZERO;

    public LiasseTableauT14AffectationDTO() {}

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getReportANouveauAnterieur() { return reportANouveauAnterieur; }
    public void setReportANouveauAnterieur(BigDecimal reportANouveauAnterieur) { this.reportANouveauAnterieur = reportANouveauAnterieur; }

    public BigDecimal getResultatNetExercicePrecedent() { return resultatNetExercicePrecedent; }
    public void setResultatNetExercicePrecedent(BigDecimal resultatNetExercicePrecedent) { this.resultatNetExercicePrecedent = resultatNetExercicePrecedent; }

    public BigDecimal getPrelevementsSurReserves() { return prelevementsSurReserves; }
    public void setPrelevementsSurReserves(BigDecimal prelevementsSurReserves) { this.prelevementsSurReserves = prelevementsSurReserves; }

    public BigDecimal getTotalOrigine() { return totalOrigine; }
    public void setTotalOrigine(BigDecimal totalOrigine) { this.totalOrigine = totalOrigine; }

    public BigDecimal getReserveLegale() { return reserveLegale; }
    public void setReserveLegale(BigDecimal reserveLegale) { this.reserveLegale = reserveLegale; }

    public BigDecimal getAutresReserves() { return autresReserves; }
    public void setAutresReserves(BigDecimal autresReserves) { this.autresReserves = autresReserves; }

    public BigDecimal getDividendesDistribues() { return dividendesDistribues; }
    public void setDividendesDistribues(BigDecimal dividendesDistribues) { this.dividendesDistribues = dividendesDistribues; }

    public BigDecimal getAutresAffectations() { return autresAffectations; }
    public void setAutresAffectations(BigDecimal autresAffectations) { this.autresAffectations = autresAffectations; }

    public BigDecimal getReportANouveauSolde() { return reportANouveauSolde; }
    public void setReportANouveauSolde(BigDecimal reportANouveauSolde) { this.reportANouveauSolde = reportANouveauSolde; }

    public BigDecimal getTotalAffectations() { return totalAffectations; }
    public void setTotalAffectations(BigDecimal totalAffectations) { this.totalAffectations = totalAffectations; }
}
