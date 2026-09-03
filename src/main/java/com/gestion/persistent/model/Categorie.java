package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
public class Categorie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String code;

    private String description;

    private String couleur; // Ex: #3b82f6 pour l'affichage POS/badge

    private String icone; // Nom d'icône pour l'interface

    @Column(nullable = false)
    private Boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"parent", "sousCategories", "hibernateLazyInitializer", "handler"})
    private Categorie parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private java.util.List<Categorie> sousCategories = new java.util.ArrayList<>();

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Categorie() {
    }

    public Categorie(String nom, String code, Long pointDeVenteId) {
        this.nom = nom;
        this.code = code;
        this.pointDeVenteId = pointDeVenteId;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getIcone() {
        return icone;
    }

    public void setIcone(String icone) {
        this.icone = icone;
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

    public Categorie getParent() {
        return parent;
    }

    public void setParent(Categorie parent) {
        this.parent = parent;
    }

    public java.util.List<Categorie> getSousCategories() {
        return sousCategories;
    }

    public void setSousCategories(java.util.List<Categorie> sousCategories) {
        this.sousCategories = sousCategories;
    }

    public String getCheminComplet() {
        if (parent != null) {
            return parent.getCheminComplet() + " > " + this.nom;
        }
        return this.nom;
    }

    public int getNiveau() {
        if (parent != null) {
            return parent.getNiveau() + 1;
        }
        return 0;
    }
}
