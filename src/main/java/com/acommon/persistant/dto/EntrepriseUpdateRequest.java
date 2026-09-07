package com.acommon.persistant.dto;

import jakarta.validation.constraints.NotBlank;

public class EntrepriseUpdateRequest {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    private String activite;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String email;
    private String siteWeb;
    private String devise;

    private String registreCommerce;
    private String numeroIdentificationFiscale;
    private String numeroIdentificationStatistique;
    private String articleImposition;
    private String compteBancaireRib;
    private String nomBanque;
    private Boolean actif;

    public EntrepriseUpdateRequest() {}

    // Getters and Setters

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
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

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public String getRegistreCommerce() {
        return registreCommerce;
    }

    public void setRegistreCommerce(String registreCommerce) {
        this.registreCommerce = registreCommerce;
    }

    public String getNumeroIdentificationFiscale() {
        return numeroIdentificationFiscale;
    }

    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) {
        this.numeroIdentificationFiscale = numeroIdentificationFiscale;
    }

    public String getNumeroIdentificationStatistique() {
        return numeroIdentificationStatistique;
    }

    public void setNumeroIdentificationStatistique(String numeroIdentificationStatistique) {
        this.numeroIdentificationStatistique = numeroIdentificationStatistique;
    }

    public String getArticleImposition() {
        return articleImposition;
    }

    public void setArticleImposition(String articleImposition) {
        this.articleImposition = articleImposition;
    }

    public String getCompteBancaireRib() {
        return compteBancaireRib;
    }

    public void setCompteBancaireRib(String compteBancaireRib) {
        this.compteBancaireRib = compteBancaireRib;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}
