package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LiasseTableauT4TfrDTO {

    private String titre = "TABLEAU 4 : TABLEAU DE FORMATION DES RÉSULTATS (T.F.R.)";
    private Integer annee;

    // 1. Marge brute
    private BigDecimal ventesMarchandises = BigDecimal.ZERO;
    private BigDecimal achatsRevendusMarchandises = BigDecimal.ZERO;
    private BigDecimal margeBrute = BigDecimal.ZERO;

    // 2. Production de l'exercice
    private BigDecimal ventesBiensEtServices = BigDecimal.ZERO;
    private BigDecimal variationStocksProduits = BigDecimal.ZERO;
    private BigDecimal immobilisationsProduites = BigDecimal.ZERO;
    private BigDecimal productionExercice = BigDecimal.ZERO;

    // 3. Consommation de l'exercice
    private BigDecimal achatsConsommesMatieres = BigDecimal.ZERO;
    private BigDecimal autresChargesExternes = BigDecimal.ZERO;
    private BigDecimal consommationExercice = BigDecimal.ZERO;

    // 4. Valeur Ajoutée (VA)
    private BigDecimal valeurAjoutee = BigDecimal.ZERO;

    // 5. Excédent Brut d'Exploitation (EBE)
    private BigDecimal subventionsExploitation = BigDecimal.ZERO;
    private BigDecimal impotsEtTaxes = BigDecimal.ZERO;
    private BigDecimal chargesPersonnel = BigDecimal.ZERO;
    private BigDecimal excedentBrutExploitation = BigDecimal.ZERO;

    // 6. Résultat d'exploitation
    private BigDecimal autresProduitsExploitation = BigDecimal.ZERO;
    private BigDecimal autresChargesExploitation = BigDecimal.ZERO;
    private BigDecimal reprisesExploitation = BigDecimal.ZERO;
    private BigDecimal dotationsExploitation = BigDecimal.ZERO;
    private BigDecimal resultatExploitation = BigDecimal.ZERO;

    // 7. Résultat financier
    private BigDecimal produitsFinanciers = BigDecimal.ZERO;
    private BigDecimal chargesFinancieres = BigDecimal.ZERO;
    private BigDecimal resultatFinancier = BigDecimal.ZERO;

    // 8. Résultat courant
    private BigDecimal resultatCourant = BigDecimal.ZERO;

    // 9. Résultat non courant
    private BigDecimal produitsNonCourants = BigDecimal.ZERO;
    private BigDecimal chargesNonCourantes = BigDecimal.ZERO;
    private BigDecimal resultatNonCourant = BigDecimal.ZERO;

    // 10. Impôts sur les résultats
    private BigDecimal impotsSurResultats = BigDecimal.ZERO;

    // 11. Résultat net de l'exercice
    private BigDecimal resultatNet = BigDecimal.ZERO;

    public LiasseTableauT4TfrDTO() {}

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getVentesMarchandises() { return ventesMarchandises; }
    public void setVentesMarchandises(BigDecimal ventesMarchandises) { this.ventesMarchandises = ventesMarchandises; }

    public BigDecimal getAchatsRevendusMarchandises() { return achatsRevendusMarchandises; }
    public void setAchatsRevendusMarchandises(BigDecimal achatsRevendusMarchandises) { this.achatsRevendusMarchandises = achatsRevendusMarchandises; }

    public BigDecimal getMargeBrute() { return margeBrute; }
    public void setMargeBrute(BigDecimal margeBrute) { this.margeBrute = margeBrute; }

    public BigDecimal getVentesBiensEtServices() { return ventesBiensEtServices; }
    public void setVentesBiensEtServices(BigDecimal ventesBiensEtServices) { this.ventesBiensEtServices = ventesBiensEtServices; }

    public BigDecimal getVariationStocksProduits() { return variationStocksProduits; }
    public void setVariationStocksProduits(BigDecimal variationStocksProduits) { this.variationStocksProduits = variationStocksProduits; }

    public BigDecimal getImmobilisationsProduites() { return immobilisationsProduites; }
    public void setImmobilisationsProduites(BigDecimal immobilisationsProduites) { this.immobilisationsProduites = immobilisationsProduites; }

    public BigDecimal getProductionExercice() { return productionExercice; }
    public void setProductionExercice(BigDecimal productionExercice) { this.productionExercice = productionExercice; }

    public BigDecimal getAchatsConsommesMatieres() { return achatsConsommesMatieres; }
    public void setAchatsConsommesMatieres(BigDecimal achatsConsommesMatieres) { this.achatsConsommesMatieres = achatsConsommesMatieres; }

    public BigDecimal getAutresChargesExternes() { return autresChargesExternes; }
    public void setAutresChargesExternes(BigDecimal autresChargesExternes) { this.autresChargesExternes = autresChargesExternes; }

    public BigDecimal getConsommationExercice() { return consommationExercice; }
    public void setConsommationExercice(BigDecimal consommationExercice) { this.consommationExercice = consommationExercice; }

    public BigDecimal getValeurAjoutee() { return valeurAjoutee; }
    public void setValeurAjoutee(BigDecimal valeurAjoutee) { this.valeurAjoutee = valeurAjoutee; }

    public BigDecimal getSubventionsExploitation() { return subventionsExploitation; }
    public void setSubventionsExploitation(BigDecimal subventionsExploitation) { this.subventionsExploitation = subventionsExploitation; }

    public BigDecimal getImpotsEtTaxes() { return impotsEtTaxes; }
    public void setImpotsEtTaxes(BigDecimal impotsEtTaxes) { this.impotsEtTaxes = impotsEtTaxes; }

    public BigDecimal getChargesPersonnel() { return chargesPersonnel; }
    public void setChargesPersonnel(BigDecimal chargesPersonnel) { this.chargesPersonnel = chargesPersonnel; }

    public BigDecimal getExcedentBrutExploitation() { return excedentBrutExploitation; }
    public void setExcedentBrutExploitation(BigDecimal excedentBrutExploitation) { this.excedentBrutExploitation = excedentBrutExploitation; }

    public BigDecimal getAutresProduitsExploitation() { return autresProduitsExploitation; }
    public void setAutresProduitsExploitation(BigDecimal autresProduitsExploitation) { this.autresProduitsExploitation = autresProduitsExploitation; }

    public BigDecimal getAutresChargesExploitation() { return autresChargesExploitation; }
    public void setAutresChargesExploitation(BigDecimal autresChargesExploitation) { this.autresChargesExploitation = autresChargesExploitation; }

    public BigDecimal getReprisesExploitation() { return reprisesExploitation; }
    public void setReprisesExploitation(BigDecimal reprisesExploitation) { this.reprisesExploitation = reprisesExploitation; }

    public BigDecimal getDotationsExploitation() { return dotationsExploitation; }
    public void setDotationsExploitation(BigDecimal dotationsExploitation) { this.dotationsExploitation = dotationsExploitation; }

    public BigDecimal getResultatExploitation() { return resultatExploitation; }
    public void setResultatExploitation(BigDecimal resultatExploitation) { this.resultatExploitation = resultatExploitation; }

    public BigDecimal getProduitsFinanciers() { return produitsFinanciers; }
    public void setProduitsFinanciers(BigDecimal produitsFinanciers) { this.produitsFinanciers = produitsFinanciers; }

    public BigDecimal getChargesFinancieres() { return chargesFinancieres; }
    public void setChargesFinancieres(BigDecimal chargesFinancieres) { this.chargesFinancieres = chargesFinancieres; }

    public BigDecimal getResultatFinancier() { return resultatFinancier; }
    public void setResultatFinancier(BigDecimal resultatFinancier) { this.resultatFinancier = resultatFinancier; }

    public BigDecimal getResultatCourant() { return resultatCourant; }
    public void setResultatCourant(BigDecimal resultatCourant) { this.resultatCourant = resultatCourant; }

    public BigDecimal getProduitsNonCourants() { return produitsNonCourants; }
    public void setProduitsNonCourants(BigDecimal produitsNonCourants) { this.produitsNonCourants = produitsNonCourants; }

    public BigDecimal getChargesNonCourantes() { return chargesNonCourantes; }
    public void setChargesNonCourantes(BigDecimal chargesNonCourantes) { this.chargesNonCourantes = chargesNonCourantes; }

    public BigDecimal getResultatNonCourant() { return resultatNonCourant; }
    public void setResultatNonCourant(BigDecimal resultatNonCourant) { this.resultatNonCourant = resultatNonCourant; }

    public BigDecimal getImpotsSurResultats() { return impotsSurResultats; }
    public void setImpotsSurResultats(BigDecimal impotsSurResultats) { this.impotsSurResultats = impotsSurResultats; }

    public BigDecimal getResultatNet() { return resultatNet; }
    public void setResultatNet(BigDecimal resultatNet) { this.resultatNet = resultatNet; }
}
