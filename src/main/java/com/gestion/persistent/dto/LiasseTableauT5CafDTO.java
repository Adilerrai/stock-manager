package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LiasseTableauT5CafDTO {

    private String titre = "TABLEAU 5 : CAPACITÉ D'AUTOFINANCEMENT (C.A.F.) - AUTOFINANCEMENT";
    private Integer annee;

    // Éléments de calcul
    private BigDecimal resultatNet = BigDecimal.ZERO;
    private BigDecimal dotationsExploitation = BigDecimal.ZERO;
    private BigDecimal dotationsFinancieres = BigDecimal.ZERO;
    private BigDecimal dotationsNonCourantes = BigDecimal.ZERO;
    private BigDecimal totalDotations = BigDecimal.ZERO;

    private BigDecimal reprisesExploitation = BigDecimal.ZERO;
    private BigDecimal reprisesFinancieres = BigDecimal.ZERO;
    private BigDecimal reprisesNonCourantes = BigDecimal.ZERO;
    private BigDecimal totalReprises = BigDecimal.ZERO;

    private BigDecimal produitsCessionsImmobilisations = BigDecimal.ZERO; // 7513
    private BigDecimal vnaImmobilisationsCedeess = BigDecimal.ZERO; // 6513

    // Résultats ESG
    private BigDecimal capaciteAutofinancement = BigDecimal.ZERO; // CAF
    private BigDecimal distributionsDividendes = BigDecimal.ZERO;
    private BigDecimal autofinancement = BigDecimal.ZERO;

    public LiasseTableauT5CafDTO() {}

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getResultatNet() { return resultatNet; }
    public void setResultatNet(BigDecimal resultatNet) { this.resultatNet = resultatNet; }

    public BigDecimal getDotationsExploitation() { return dotationsExploitation; }
    public void setDotationsExploitation(BigDecimal dotationsExploitation) { this.dotationsExploitation = dotationsExploitation; }

    public BigDecimal getDotationsFinancieres() { return dotationsFinancieres; }
    public void setDotationsFinancieres(BigDecimal dotationsFinancieres) { this.dotationsFinancieres = dotationsFinancieres; }

    public BigDecimal getDotationsNonCourantes() { return dotationsNonCourantes; }
    public void setDotationsNonCourantes(BigDecimal dotationsNonCourantes) { this.dotationsNonCourantes = dotationsNonCourantes; }

    public BigDecimal getTotalDotations() { return totalDotations; }
    public void setTotalDotations(BigDecimal totalDotations) { this.totalDotations = totalDotations; }

    public BigDecimal getReprisesExploitation() { return reprisesExploitation; }
    public void setReprisesExploitation(BigDecimal reprisesExploitation) { this.reprisesExploitation = reprisesExploitation; }

    public BigDecimal getReprisesFinancieres() { return reprisesFinancieres; }
    public void setReprisesFinancieres(BigDecimal reprisesFinancieres) { this.reprisesFinancieres = reprisesFinancieres; }

    public BigDecimal getReprisesNonCourantes() { return reprisesNonCourantes; }
    public void setReprisesNonCourantes(BigDecimal reprisesNonCourantes) { this.reprisesNonCourantes = reprisesNonCourantes; }

    public BigDecimal getTotalReprises() { return totalReprises; }
    public void setTotalReprises(BigDecimal totalReprises) { this.totalReprises = totalReprises; }

    public BigDecimal getProduitsCessionsImmobilisations() { return produitsCessionsImmobilisations; }
    public void setProduitsCessionsImmobilisations(BigDecimal produitsCessionsImmobilisations) { this.produitsCessionsImmobilisations = produitsCessionsImmobilisations; }

    public BigDecimal getVnaImmobilisationsCedeess() { return vnaImmobilisationsCedeess; }
    public void setVnaImmobilisationsCedeess(BigDecimal vnaImmobilisationsCedeess) { this.vnaImmobilisationsCedeess = vnaImmobilisationsCedeess; }

    public BigDecimal getCapaciteAutofinancement() { return capaciteAutofinancement; }
    public void setCapaciteAutofinancement(BigDecimal capaciteAutofinancement) { this.capaciteAutofinancement = capaciteAutofinancement; }

    public BigDecimal getDistributionsDividendes() { return distributionsDividendes; }
    public void setDistributionsDividendes(BigDecimal distributionsDividendes) { this.distributionsDividendes = distributionsDividendes; }

    public BigDecimal getAutofinancement() { return autofinancement; }
    public void setAutofinancement(BigDecimal autofinancement) { this.autofinancement = autofinancement; }
}
