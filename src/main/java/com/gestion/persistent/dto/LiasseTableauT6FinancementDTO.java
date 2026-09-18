package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LiasseTableauT6FinancementDTO {

    private String titre = "TABLEAU 6 : TABLEAU DE FINANCEMENT DE L'EXERCICE";
    private Integer annee;

    // I. Ressources stables
    private BigDecimal autofinancement = BigDecimal.ZERO;
    private BigDecimal cessionsImmobilisations = BigDecimal.ZERO;
    private BigDecimal augmentationsCapital = BigDecimal.ZERO;
    private BigDecimal nouveauxEmprunts = BigDecimal.ZERO;
    private BigDecimal totalRessourcesStables = BigDecimal.ZERO;

    // II. Emplois stables
    private BigDecimal acquisitionsImmobilisations = BigDecimal.ZERO;
    private BigDecimal remboursementsEmprunts = BigDecimal.ZERO;
    private BigDecimal immobilisationsNonValeurs = BigDecimal.ZERO;
    private BigDecimal totalEmploisStables = BigDecimal.ZERO;

    // III. Variation du Fonds de Roulement Fonctionnel (FRF)
    private BigDecimal variationFondsDeRoulement = BigDecimal.ZERO;

    // IV. Variation du Besoin en Financement Global (BFG)
    private BigDecimal variationActifCirculant = BigDecimal.ZERO;
    private BigDecimal variationPassifCirculant = BigDecimal.ZERO;
    private BigDecimal variationBesoinFinancementGlobal = BigDecimal.ZERO;

    // V. Variation de la Trésorerie Nette
    private BigDecimal tresorerieDebut = BigDecimal.ZERO;
    private BigDecimal tresorerieFin = BigDecimal.ZERO;
    private BigDecimal variationTresorerieNette = BigDecimal.ZERO;

    public LiasseTableauT6FinancementDTO() {}

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getAutofinancement() { return autofinancement; }
    public void setAutofinancement(BigDecimal autofinancement) { this.autofinancement = autofinancement; }

    public BigDecimal getCessionsImmobilisations() { return cessionsImmobilisations; }
    public void setCessionsImmobilisations(BigDecimal cessionsImmobilisations) { this.cessionsImmobilisations = cessionsImmobilisations; }

    public BigDecimal getAugmentationsCapital() { return augmentationsCapital; }
    public void setAugmentationsCapital(BigDecimal augmentationsCapital) { this.augmentationsCapital = augmentationsCapital; }

    public BigDecimal getNouveauxEmprunts() { return nouveauxEmprunts; }
    public void setNouveauxEmprunts(BigDecimal nouveauxEmprunts) { this.nouveauxEmprunts = nouveauxEmprunts; }

    public BigDecimal getTotalRessourcesStables() { return totalRessourcesStables; }
    public void setTotalRessourcesStables(BigDecimal totalRessourcesStables) { this.totalRessourcesStables = totalRessourcesStables; }

    public BigDecimal getAcquisitionsImmobilisations() { return acquisitionsImmobilisations; }
    public void setAcquisitionsImmobilisations(BigDecimal acquisitionsImmobilisations) { this.acquisitionsImmobilisations = acquisitionsImmobilisations; }

    public BigDecimal getRemboursementsEmprunts() { return remboursementsEmprunts; }
    public void setRemboursementsEmprunts(BigDecimal remboursementsEmprunts) { this.remboursementsEmprunts = remboursementsEmprunts; }

    public BigDecimal getImmobilisationsNonValeurs() { return immobilisationsNonValeurs; }
    public void setImmobilisationsNonValeurs(BigDecimal immobilisationsNonValeurs) { this.immobilisationsNonValeurs = immobilisationsNonValeurs; }

    public BigDecimal getTotalEmploisStables() { return totalEmploisStables; }
    public void setTotalEmploisStables(BigDecimal totalEmploisStables) { this.totalEmploisStables = totalEmploisStables; }

    public BigDecimal getVariationFondsDeRoulement() { return variationFondsDeRoulement; }
    public void setVariationFondsDeRoulement(BigDecimal variationFondsDeRoulement) { this.variationFondsDeRoulement = variationFondsDeRoulement; }

    public BigDecimal getVariationActifCirculant() { return variationActifCirculant; }
    public void setVariationActifCirculant(BigDecimal variationActifCirculant) { this.variationActifCirculant = variationActifCirculant; }

    public BigDecimal getVariationPassifCirculant() { return variationPassifCirculant; }
    public void setVariationPassifCirculant(BigDecimal variationPassifCirculant) { this.variationPassifCirculant = variationPassifCirculant; }

    public BigDecimal getVariationBesoinFinancementGlobal() { return variationBesoinFinancementGlobal; }
    public void setVariationBesoinFinancementGlobal(BigDecimal variationBesoinFinancementGlobal) { this.variationBesoinFinancementGlobal = variationBesoinFinancementGlobal; }

    public BigDecimal getTresorerieDebut() { return tresorerieDebut; }
    public void setTresorerieDebut(BigDecimal tresorerieDebut) { this.tresorerieDebut = tresorerieDebut; }

    public BigDecimal getTresorerieFin() { return tresorerieFin; }
    public void setTresorerieFin(BigDecimal tresorerieFin) { this.tresorerieFin = tresorerieFin; }

    public BigDecimal getVariationTresorerieNette() { return variationTresorerieNette; }
    public void setVariationTresorerieNette(BigDecimal variationTresorerieNette) { this.variationTresorerieNette = variationTresorerieNette; }
}
