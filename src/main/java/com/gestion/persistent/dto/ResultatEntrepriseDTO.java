package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ResultatEntrepriseDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Produits / Ventes
    private BigDecimal chiffreAffairesHT = BigDecimal.ZERO;
    private BigDecimal coutMarchandisesHT = BigDecimal.ZERO;
    private BigDecimal totalRemises = BigDecimal.ZERO;
    private BigDecimal totalRetours = BigDecimal.ZERO;
    private BigDecimal margeCommerciale = BigDecimal.ZERO;
    private BigDecimal tauxMarge = BigDecimal.ZERO; // %

    // Charges / Dépenses
    private BigDecimal totalDepenses = BigDecimal.ZERO;
    private Map<String, BigDecimal> depensesParCategorie;

    // Résultat d'exploitation estimé
    private BigDecimal resultatNetEstime = BigDecimal.ZERO;
    private BigDecimal rentabiliteNette = BigDecimal.ZERO; // % sur CA

    public ResultatEntrepriseDTO() {}

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getChiffreAffairesHT() { return chiffreAffairesHT; }
    public void setChiffreAffairesHT(BigDecimal chiffreAffairesHT) { this.chiffreAffairesHT = chiffreAffairesHT; }

    public BigDecimal getCoutMarchandisesHT() { return coutMarchandisesHT; }
    public void setCoutMarchandisesHT(BigDecimal coutMarchandisesHT) { this.coutMarchandisesHT = coutMarchandisesHT; }

    public BigDecimal getTotalRemises() { return totalRemises; }
    public void setTotalRemises(BigDecimal totalRemises) { this.totalRemises = totalRemises; }

    public BigDecimal getTotalRetours() { return totalRetours; }
    public void setTotalRetours(BigDecimal totalRetours) { this.totalRetours = totalRetours; }

    public BigDecimal getMargeCommerciale() { return margeCommerciale; }
    public void setMargeCommerciale(BigDecimal margeCommerciale) { this.margeCommerciale = margeCommerciale; }

    public BigDecimal getTauxMarge() { return tauxMarge; }
    public void setTauxMarge(BigDecimal tauxMarge) { this.tauxMarge = tauxMarge; }

    public BigDecimal getTotalDepenses() { return totalDepenses; }
    public void setTotalDepenses(BigDecimal totalDepenses) { this.totalDepenses = totalDepenses; }

    public Map<String, BigDecimal> getDepensesParCategorie() { return depensesParCategorie; }
    public void setDepensesParCategorie(Map<String, BigDecimal> depensesParCategorie) { this.depensesParCategorie = depensesParCategorie; }

    public BigDecimal getResultatNetEstime() { return resultatNetEstime; }
    public void setResultatNetEstime(BigDecimal resultatNetEstime) { this.resultatNetEstime = resultatNetEstime; }

    public BigDecimal getRentabiliteNette() { return rentabiliteNette; }
    public void setRentabiliteNette(BigDecimal rentabiliteNette) { this.rentabiliteNette = rentabiliteNette; }
}
