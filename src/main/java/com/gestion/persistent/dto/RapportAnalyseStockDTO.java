package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RapportAnalyseStockDTO {
    // 1. Valeur financière et références
    private BigDecimal valeurTotaleStockPmp = BigDecimal.ZERO;
    private long nombreReferencesEnStock = 0;
    private long nombreReferencesTotalCatalogue = 0;
    private long nombreReferencesAlerteSeuilBas = 0;

    // 2. Vitesse et rotation moyenne
    private BigDecimal tauxRotationMoyen = BigDecimal.ZERO;
    private int delaiMoyenEcoulementJours = 0;
    private BigDecimal sortiesAnnuellesEstimeesDH = BigDecimal.ZERO;

    // 3. Synthèse des comportements décisionnels
    private long nombreSousStock = 0;
    private long nombreSurStock = 0;
    private long nombreNormal = 0;
    private long nombreRupture = 0;

    // 4. Répartition par catégorie
    private List<ValorisationCategorieDTO> categories = new ArrayList<>();

    // 5. Matrice croisée ABC x Rotation
    private MatriceAbcRotationDTO matriceAbcRotation = new MatriceAbcRotationDTO();

    // 6. Articles détaillés
    private List<AnalyseProduitStockDTO> produits = new ArrayList<>();

    public RapportAnalyseStockDTO() {}

    public BigDecimal getValeurTotaleStockPmp() { return valeurTotaleStockPmp; }
    public void setValeurTotaleStockPmp(BigDecimal valeurTotaleStockPmp) { this.valeurTotaleStockPmp = valeurTotaleStockPmp; }

    public long getNombreReferencesEnStock() { return nombreReferencesEnStock; }
    public void setNombreReferencesEnStock(long nombreReferencesEnStock) { this.nombreReferencesEnStock = nombreReferencesEnStock; }

    public long getNombreReferencesTotalCatalogue() { return nombreReferencesTotalCatalogue; }
    public void setNombreReferencesTotalCatalogue(long nombreReferencesTotalCatalogue) { this.nombreReferencesTotalCatalogue = nombreReferencesTotalCatalogue; }

    public long getNombreReferencesAlerteSeuilBas() { return nombreReferencesAlerteSeuilBas; }
    public void setNombreReferencesAlerteSeuilBas(long nombreReferencesAlerteSeuilBas) { this.nombreReferencesAlerteSeuilBas = nombreReferencesAlerteSeuilBas; }

    public BigDecimal getTauxRotationMoyen() { return tauxRotationMoyen; }
    public void setTauxRotationMoyen(BigDecimal tauxRotationMoyen) { this.tauxRotationMoyen = tauxRotationMoyen; }

    public int getDelaiMoyenEcoulementJours() { return delaiMoyenEcoulementJours; }
    public void setDelaiMoyenEcoulementJours(int delaiMoyenEcoulementJours) { this.delaiMoyenEcoulementJours = delaiMoyenEcoulementJours; }

    public BigDecimal getSortiesAnnuellesEstimeesDH() { return sortiesAnnuellesEstimeesDH; }
    public void setSortiesAnnuellesEstimeesDH(BigDecimal sortiesAnnuellesEstimeesDH) { this.sortiesAnnuellesEstimeesDH = sortiesAnnuellesEstimeesDH; }

    public long getNombreSousStock() { return nombreSousStock; }
    public void setNombreSousStock(long nombreSousStock) { this.nombreSousStock = nombreSousStock; }

    public long getNombreSurStock() { return nombreSurStock; }
    public void setNombreSurStock(long nombreSurStock) { this.nombreSurStock = nombreSurStock; }

    public long getNombreNormal() { return nombreNormal; }
    public void setNombreNormal(long nombreNormal) { this.nombreNormal = nombreNormal; }

    public long getNombreRupture() { return nombreRupture; }
    public void setNombreRupture(long nombreRupture) { this.nombreRupture = nombreRupture; }

    public List<ValorisationCategorieDTO> getCategories() { return categories; }
    public void setCategories(List<ValorisationCategorieDTO> categories) { this.categories = categories; }

    public MatriceAbcRotationDTO getMatriceAbcRotation() { return matriceAbcRotation; }
    public void setMatriceAbcRotation(MatriceAbcRotationDTO matriceAbcRotation) { this.matriceAbcRotation = matriceAbcRotation; }

    public List<AnalyseProduitStockDTO> getProduits() { return produits; }
    public void setProduits(List<AnalyseProduitStockDTO> produits) { this.produits = produits; }
}
