package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CategorieClient;
import com.gestion.persistent.enums.TarifClient;

public class ClientSearchCriteria {
    private String query;
    private String nom;
    private String prenom;
    private String telephone;
    private String email;
    private String ice;
    private CategorieClient categorie;
    private TarifClient tarif;
    private Long commercialId;
    private Boolean actif;
    private Boolean depassementCredit;

    public ClientSearchCriteria() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public CategorieClient getCategorie() { return categorie; }
    public void setCategorie(CategorieClient categorie) { this.categorie = categorie; }

    public TarifClient getTarif() { return tarif; }
    public void setTarif(TarifClient tarif) { this.tarif = tarif; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Boolean getDepassementCredit() { return depassementCredit; }
    public void setDepassementCredit(Boolean depassementCredit) { this.depassementCredit = depassementCredit; }
}
