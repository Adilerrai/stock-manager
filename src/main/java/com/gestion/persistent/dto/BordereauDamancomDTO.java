package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BordereauDamancomDTO {

    private int annee;
    private int mois; // 1 à 12
    private String numeroAffiliationEntreprise; // N° CNSS employeur (7 chiffres)
    private String raisonSociale;
    private String ice;
    private LocalDate dateGeneration;

    // Totaux
    private int totalSalaries = 0;
    private BigDecimal totalSalairesBruts = BigDecimal.ZERO;
    private BigDecimal totalSalairesPlafonnes = BigDecimal.ZERO;

    // Cotisations CNSS
    private BigDecimal totalAllocationsFamiliales = BigDecimal.ZERO; // 6.40% patronal
    private BigDecimal totalPrestationsSociales = BigDecimal.ZERO;   // 8.98% (4.48% salarial + 8.98% patronal)
    private BigDecimal totalTaxeFormationPro = BigDecimal.ZERO;      // 1.60% patronal
    private BigDecimal totalCotisationsAmo = BigDecimal.ZERO;        // 4.11% patronal + 2.26% salarial = 6.37%

    private BigDecimal totalCotisationsSalariales = BigDecimal.ZERO;
    private BigDecimal totalCotisationsPatronales = BigDecimal.ZERO;
    private BigDecimal totalGlobalAPayerCnss = BigDecimal.ZERO;

    private List<LigneBdsDamancomDTO> lignes = new ArrayList<>();

    public BordereauDamancomDTO() {
        this.dateGeneration = LocalDate.now();
    }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }

    public String getNumeroAffiliationEntreprise() { return numeroAffiliationEntreprise; }
    public void setNumeroAffiliationEntreprise(String numeroAffiliationEntreprise) { this.numeroAffiliationEntreprise = numeroAffiliationEntreprise; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public LocalDate getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDate dateGeneration) { this.dateGeneration = dateGeneration; }

    public int getTotalSalaries() { return totalSalaries; }
    public void setTotalSalaries(int totalSalaries) { this.totalSalaries = totalSalaries; }

    public BigDecimal getTotalSalairesBruts() { return totalSalairesBruts; }
    public void setTotalSalairesBruts(BigDecimal totalSalairesBruts) { this.totalSalairesBruts = totalSalairesBruts; }

    public BigDecimal getTotalSalairesPlafonnes() { return totalSalairesPlafonnes; }
    public void setTotalSalairesPlafonnes(BigDecimal totalSalairesPlafonnes) { this.totalSalairesPlafonnes = totalSalairesPlafonnes; }

    public BigDecimal getTotalAllocationsFamiliales() { return totalAllocationsFamiliales; }
    public void setTotalAllocationsFamiliales(BigDecimal totalAllocationsFamiliales) { this.totalAllocationsFamiliales = totalAllocationsFamiliales; }

    public BigDecimal getTotalPrestationsSociales() { return totalPrestationsSociales; }
    public void setTotalPrestationsSociales(BigDecimal totalPrestationsSociales) { this.totalPrestationsSociales = totalPrestationsSociales; }

    public BigDecimal getTotalTaxeFormationPro() { return totalTaxeFormationPro; }
    public void setTotalTaxeFormationPro(BigDecimal totalTaxeFormationPro) { this.totalTaxeFormationPro = totalTaxeFormationPro; }

    public BigDecimal getTotalCotisationsAmo() { return totalCotisationsAmo; }
    public void setTotalCotisationsAmo(BigDecimal totalCotisationsAmo) { this.totalCotisationsAmo = totalCotisationsAmo; }

    public BigDecimal getTotalCotisationsSalariales() { return totalCotisationsSalariales; }
    public void setTotalCotisationsSalariales(BigDecimal totalCotisationsSalariales) { this.totalCotisationsSalariales = totalCotisationsSalariales; }

    public BigDecimal getTotalCotisationsPatronales() { return totalCotisationsPatronales; }
    public void setTotalCotisationsPatronales(BigDecimal totalCotisationsPatronales) { this.totalCotisationsPatronales = totalCotisationsPatronales; }

    public BigDecimal getTotalGlobalAPayerCnss() { return totalGlobalAPayerCnss; }
    public void setTotalGlobalAPayerCnss(BigDecimal totalGlobalAPayerCnss) { this.totalGlobalAPayerCnss = totalGlobalAPayerCnss; }

    public List<LigneBdsDamancomDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneBdsDamancomDTO> lignes) { this.lignes = lignes; }
}
