package com.acommon.persistant.dto;

import jakarta.validation.constraints.NotBlank;

public class PointDeVenteRequest {

    @NotBlank(message = "Le nom du point de vente est obligatoire")
    private String nomPointDeVente;

    private String adresse;
    private String telephone;
    private String email;
    private Boolean actif = true;

    public PointDeVenteRequest() {}

    public String getNomPointDeVente() {
        return nomPointDeVente;
    }

    public void setNomPointDeVente(String nomPointDeVente) {
        this.nomPointDeVente = nomPointDeVente;
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

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
