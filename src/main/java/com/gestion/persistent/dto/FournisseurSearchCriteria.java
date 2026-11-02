package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.Incoterm;
import com.gestion.persistent.enums.TypeFournisseur;

/**
 * Critères de recherche multicritère des fournisseurs (POST /api/v1/fournisseurs/search).
 *
 * <ul>
 *   <li>{@code query} : recherche globale (OU) sur raison sociale, contact, téléphone, email, adresse, ville, ICE, RC et IF.</li>
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

    // Fiscalité & légalité
    private String numeroRegistreCommerce;
    private String numeroIdentificationFiscale;
    private String patente;

    // Localisation complémentaire
    private String pays;
    private String codePostal;

    // Conditions d'achat & banque
    private String banqueNom;
    private String conditionsPaiement;
    private Integer delaiPaiementMin;
    private Integer delaiPaiementMax;
    private Incoterm incotermDefaut;

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

    public String getNumeroRegistreCommerce() { return numeroRegistreCommerce; }
    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) { this.numeroRegistreCommerce = numeroRegistreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }

    public Integer getDelaiPaiementMin() { return delaiPaiementMin; }
    public void setDelaiPaiementMin(Integer delaiPaiementMin) { this.delaiPaiementMin = delaiPaiementMin; }

    public Integer getDelaiPaiementMax() { return delaiPaiementMax; }
    public void setDelaiPaiementMax(Integer delaiPaiementMax) { this.delaiPaiementMax = delaiPaiementMax; }

    public Incoterm getIncotermDefaut() { return incotermDefaut; }
    public void setIncotermDefaut(Incoterm incotermDefaut) { this.incotermDefaut = incotermDefaut; }
}
