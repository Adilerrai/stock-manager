package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "banques")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Banque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Ex: AWB, BCP, BOA, CIH, SGMB

    @Column(nullable = false, length = 150)
    private String nom; // Ex: Attijariwafa Bank, Banque Populaire, etc.

    @Column(name = "nom_court", length = 50)
    private String nomCourt;

    @Column(name = "code_swift", length = 20)
    private String swiftBic;

    @Column(length = 255)
    private String logo;

    @Column(length = 20)
    private String couleur; // Ex: #E67E22 pour Attijariwafa, #D35400 pour BCP, #2980B9 pour BOA

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<CompteFinancier> comptes = new ArrayList<>();

    public Banque() {}

    public Banque(String code, String nom, String nomCourt, String couleur) {
        this.code = code;
        this.nom = nom;
        this.nomCourt = nomCourt;
        this.couleur = couleur;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getNomCourt() {
        return nomCourt;
    }

    public void setNomCourt(String nomCourt) {
        this.nomCourt = nomCourt;
    }

    public String getSwiftBic() {
        return swiftBic;
    }

    public void setSwiftBic(String swiftBic) {
        this.swiftBic = swiftBic;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public List<CompteFinancier> getComptes() {
        return comptes;
    }

    public void setComptes(List<CompteFinancier> comptes) {
        this.comptes = comptes;
    }
}
