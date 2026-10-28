package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class ProduitSearchCriteria {
    private String nom;
    private String designation;
    private String reference;
    private String description;
    private String search;
    private BigDecimal prixMin;
    private BigDecimal prixMax;
    private Boolean actif;
    private String categorie;
    private Long categorieId;
    private String groupeArticle;
    private Long societeId;

    public ProduitSearchCriteria() {}

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getSearch() { return search; }
    public void setSearch(String search) { this.search = search; }

    public BigDecimal getPrixMin() { return prixMin; }
    public void setPrixMin(BigDecimal prixMin) { this.prixMin = prixMin; }

    public BigDecimal getPrixMax() { return prixMax; }
    public void setPrixMax(BigDecimal prixMax) { this.prixMax = prixMax; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public String getGroupeArticle() { return groupeArticle; }
    public void setGroupeArticle(String groupeArticle) { this.groupeArticle = groupeArticle; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
