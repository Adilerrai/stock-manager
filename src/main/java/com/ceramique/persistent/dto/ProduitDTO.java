package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.UniteMesure;
import com.ceramique.persistent.model.GroupeArticle;

import java.math.BigDecimal;

public class ProduitDTO {
    private Long id;
    private String reference;
    private String designation;
    private String description;
    private UniteMesure uniteMesureStock;
    private BigDecimal prixAchat;
    private BigDecimal prixVente;
    private ProduitImageDTO image;
    private Boolean actif;
    private GroupeArticle groupeArticle;
    
    // Caractéristiques techniques
    private BigDecimal longueurCm;
    private BigDecimal largeurCm;
    private BigDecimal epaisseurMm;
    private String format;

    // Constructors
    public ProduitDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public UniteMesure getUniteMesureStock() { return uniteMesureStock; }
    public void setUniteMesureStock(UniteMesure uniteMesureStock) { this.uniteMesureStock = uniteMesureStock; }
    
    public BigDecimal getPrixAchat() { return prixAchat; }
    public void setPrixAchat(BigDecimal prixAchat) { this.prixAchat = prixAchat; }
    
    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }

    public ProduitImageDTO getImage() { return image; }
    public void setImage(ProduitImageDTO image) { this.image = image; }
    
    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
    
    public GroupeArticle getGroupeArticle() { return groupeArticle; }
    public void setGroupeArticle(GroupeArticle groupeArticle) { this.groupeArticle = groupeArticle; }
    
    public BigDecimal getLongueurCm() { return longueurCm; }
    public void setLongueurCm(BigDecimal longueurCm) { this.longueurCm = longueurCm; }
    
    public BigDecimal getLargeurCm() { return largeurCm; }
    public void setLargeurCm(BigDecimal largeurCm) { this.largeurCm = largeurCm; }
    
    public BigDecimal getEpaisseurMm() { return epaisseurMm; }
    public void setEpaisseurMm(BigDecimal epaisseurMm) { this.epaisseurMm = epaisseurMm; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}