package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class CategorieNodeDTO {
    private Long id;
    private String nom;
    private String code;
    private String description;
    private String couleur;
    private String icone;
    private Long parentId;
    private String parentNom;
    private String cheminComplet;
    private int niveau;
    private Long nombreProduits = 0L;
    private int nombreSousCategories = 0;
    private List<CategorieNodeDTO> children = new ArrayList<>();

    public CategorieNodeDTO() {}

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

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }

    public String getParentNom() { return parentNom; }
    public void setParentNom(String parentNom) { this.parentNom = parentNom; }

    public String getCheminComplet() { return cheminComplet; }
    public void setCheminComplet(String cheminComplet) { this.cheminComplet = cheminComplet; }

    public int getNiveau() { return niveau; }
    public void setNiveau(int niveau) { this.niveau = niveau; }

    public Long getNombreProduits() { return nombreProduits; }
    public void setNombreProduits(Long nombreProduits) { this.nombreProduits = nombreProduits; }

    public int getNombreSousCategories() { return nombreSousCategories; }
    public void setNombreSousCategories(int nombreSousCategories) { this.nombreSousCategories = nombreSousCategories; }

    public List<CategorieNodeDTO> getChildren() { return children; }
    public void setChildren(List<CategorieNodeDTO> children) { this.children = children; }

    public void addChild(CategorieNodeDTO child) {
        this.children.add(child);
        this.nombreSousCategories = this.children.size();
    }
}
