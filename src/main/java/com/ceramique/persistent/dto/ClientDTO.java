package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.CategorieClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ClientDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String codePostal;
    private CategorieClient categorie;
    private String numeroRegistreCommerce;
    private String numeroIdentificationFiscale;
    private BigDecimal creditAutorise;
    private BigDecimal creditUtilise;
    private BigDecimal creditDisponible; // dérivé
    private Integer pointsFidelite;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereVisite;
    private String notes;
    private Long pointDeVenteId;

    public ClientDTO() {}

    // Getters / Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public CategorieClient getCategorie() { return categorie; }
    public void setCategorie(CategorieClient categorie) { this.categorie = categorie; }

    public String getNumeroRegistreCommerce() { return numeroRegistreCommerce; }
    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) { this.numeroRegistreCommerce = numeroRegistreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public BigDecimal getCreditAutorise() { return creditAutorise; }
    public void setCreditAutorise(BigDecimal creditAutorise) { this.creditAutorise = creditAutorise; }

    public BigDecimal getCreditUtilise() { return creditUtilise; }
    public void setCreditUtilise(BigDecimal creditUtilise) { this.creditUtilise = creditUtilise; }

    public BigDecimal getCreditDisponible() { return creditDisponible; }
    public void setCreditDisponible(BigDecimal creditDisponible) { this.creditDisponible = creditDisponible; }

    public Integer getPointsFidelite() { return pointsFidelite; }
    public void setPointsFidelite(Integer pointsFidelite) { this.pointsFidelite = pointsFidelite; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateDerniereVisite() { return dateDerniereVisite; }
    public void setDateDerniereVisite(LocalDateTime dateDerniereVisite) { this.dateDerniereVisite = dateDerniereVisite; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}

