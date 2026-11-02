package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeFournisseur;

/**
 * Critères de recherche multicritère des fournisseurs (POST /api/v1/fournisseurs/search).
 *
 * <ul>
 *   <li>{@code query} : recherche globale (OU) sur raison sociale, contact, téléphone, email, adresse, ville et ICE.</li>
 *   <li>Les autres critères sont combinés entre eux par un ET.</li>
 * </ul>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FournisseurSearchCriteria {
    private String query;
    private String raisonSociale;
    private String adresse;
    private String telephone;
    private String email;
    private String contact;
    private String ville;
    private String ice;
    private String devise;
    private TypeFournisseur typeFournisseur;
    private Boolean actif;
    private Long societeId;

    public FournisseurSearchCriteria() {}

    // Getters and Setters
    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getIce() {
        return ice;
    }

    public void setIce(String ice) {
        this.ice = ice;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public TypeFournisseur getTypeFournisseur() {
        return typeFournisseur;
    }

    public void setTypeFournisseur(TypeFournisseur typeFournisseur) {
        this.typeFournisseur = typeFournisseur;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Long getSocieteId() {
        return societeId;
    }

    public void setSocieteId(Long societeId) {
        this.societeId = societeId;
    }
}
