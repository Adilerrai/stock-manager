package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "meres")
public class Mere {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom; // Nom du Cabinet Fiduciaire ou Groupe Holding

    @Column(length = 30)
    private String ice;

    @Column(length = 50)
    private String rc;

    @Column(name = "identifiant_fiscal", length = 50)
    private String identifiantFiscal;

    @Column(length = 50)
    private String patente;

    @Column(name = "forme_juridique", length = 50)
    private String formeJuridique = "SARL";

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(length = 50)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "site_web", length = 200)
    private String siteWeb;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Mere() {}

    public Mere(String nom) {
        this.nom = nom;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.actif == null) {
            this.actif = true;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getRc() { return rc; }
    public void setRc(String rc) { this.rc = rc; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getFormeJuridique() { return formeJuridique; }
    public void setFormeJuridique(String formeJuridique) { this.formeJuridique = formeJuridique; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
