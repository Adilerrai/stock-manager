package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EtiquetteCodeBarreRequest {

    /**
     * Liste des identifiants des produits à imprimer.
     * Si null ou vide, tous les produits actifs sont sélectionnés.
     */
    private List<Long> produitIds;

    /**
     * Filtre optionnel par catégorie.
     */
    private Long categorieId;

    /**
     * Modèle de rendu :
     * "grille" (ou "modele02") : style inventaire avec cartouche bleu pastel et en-tête (DÉFAUT)
     * "sticker" : étiquette épurée centrée (titre + code barre + référence)
     */
    private String modele = "grille";

    /**
     * Nombre de colonnes par page A4 (2 ou 3). Défaut = 2.
     */
    private Integer colonnes = 2;

    /**
     * Nombre d'exemplaires par produit (défaut = 1).
     */
    private Integer exemplaires = 1;

    /**
     * Afficher ou non le prix de vente TTC sur l'étiquette.
     */
    private Boolean afficherPrix = false;

    public EtiquetteCodeBarreRequest() {
    }

    public List<Long> getProduitIds() {
        return produitIds;
    }

    public void setProduitIds(List<Long> produitIds) {
        this.produitIds = produitIds;
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }

    public String getModele() {
        return (modele != null && !modele.trim().isEmpty()) ? modele : "grille";
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public Integer getColonnes() {
        return (colonnes != null && colonnes >= 1 && colonnes <= 4) ? colonnes : 2;
    }

    public void setColonnes(Integer colonnes) {
        this.colonnes = colonnes;
    }

    public Integer getExemplaires() {
        return (exemplaires != null && exemplaires > 0) ? exemplaires : 1;
    }

    public void setExemplaires(Integer exemplaires) {
        this.exemplaires = exemplaires;
    }

    public Boolean getAfficherPrix() {
        return Boolean.TRUE.equals(afficherPrix);
    }

    public void setAfficherPrix(Boolean afficherPrix) {
        this.afficherPrix = afficherPrix;
    }
}
