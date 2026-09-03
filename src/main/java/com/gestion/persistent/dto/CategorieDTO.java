package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class CategorieDTO {
    private Long id;
    private String nom;
    private String code;
    private String description;
    private String couleur;
    private String icone;
    private Boolean actif;
    private Long parentId;
    private String parentNom;
    private String cheminComplet;
    private int niveau;
    private Long pointDeVenteId;
    private LocalDateTime dateCreation;

    public CategorieDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getParentNom() { return parentNom; }
    public void setParentNom(String parentNom) { this.parentNom = parentNom; }

    public String getCheminComplet() { return cheminComplet; }
    public void setCheminComplet(String cheminComplet) { this.cheminComplet = cheminComplet; }

    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
