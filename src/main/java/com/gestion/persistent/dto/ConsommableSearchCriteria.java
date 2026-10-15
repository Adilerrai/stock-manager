package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CategorieConsommable;

public class ConsommableSearchCriteria {
    private String query;
    private String reference;
    private String designation;
    private CategorieConsommable categorie;
    private Boolean actif;
    private Boolean alerteStockSeulement;

    public ConsommableSearchCriteria() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public CategorieConsommable getCategorie() { return categorie; }
    public void setCategorie(CategorieConsommable categorie) { this.categorie = categorie; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Boolean getAlerteStockSeulement() { return alerteStockSeulement; }
    public void setAlerteStockSeulement(Boolean alerteStockSeulement) { this.alerteStockSeulement = alerteStockSeulement; }
}
