package com.gestion.persistent.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FournisseurDTO {
    private Long id;
    private String raisonSociale;
    private String adresse;
    private String telephone;
    private String email;
    @Size(max = 150, message = "Le nom du contact ne doit pas dépasser 150 caractères")
    private String contact;
    private Boolean actif;

    private String ice;
    private String numeroRegistreCommerce;
    private String numeroIdentificationFiscale;
    private String patente;
    private String ribBancaire;
    private String banqueNom;
    private Integer delaiPaiementJours;
    private String ville;
    private String codePostal;
    private String pays;
    private String conditionsPaiement;

    // Constructors
    public FournisseurDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getNom() { return raisonSociale; }
    public void setNom(String nom) { this.raisonSociale = nom; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getNumeroRegistreCommerce() { return numeroRegistreCommerce; }
    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) { this.numeroRegistreCommerce = numeroRegistreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getRibBancaire() { return ribBancaire; }
    public void setRibBancaire(String ribBancaire) { this.ribBancaire = ribBancaire; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

    public Integer getDelaiPaiementJours() { return delaiPaiementJours; }
    public void setDelaiPaiementJours(Integer delaiPaiementJours) { this.delaiPaiementJours = delaiPaiementJours; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }
}
