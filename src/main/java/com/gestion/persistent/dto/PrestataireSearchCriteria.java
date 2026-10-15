package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypePrestation;

public class PrestataireSearchCriteria {
    private String query;
    private String raisonSociale;
    private String ice;
    private String telephone;
    private String ville;
    private TypePrestation typePrestation;
    private Boolean actif;

    public PrestataireSearchCriteria() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public TypePrestation getTypePrestation() { return typePrestation; }
    public void setTypePrestation(TypePrestation typePrestation) { this.typePrestation = typePrestation; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}
