package com.gestion.persistent.dto;

import com.gestion.persistent.enums.UniteMesure;
import com.gestion.persistent.enums.CategorieArticle;
import com.gestion.persistent.model.GroupeArticle;

import java.math.BigDecimal;
import java.util.Map;

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
    private String groupeArticle;
    private String codeBarre;
    private String categorieArticle;
    
    private Map<String, Object> attributes;
    
    // Tarifs additionnels HT / TTC
    private BigDecimal prixAchatHt;
    private BigDecimal prixAchatTtc;
    private BigDecimal prixVenteHt;
    private BigDecimal prixVenteTtc;
    private Long pointDeVenteId;
    private Long categorieId;
    private CategorieDTO categorie;

    // Constructors
    public ProduitDTO() {
    }

    public Long getCategorieId() {
        return categorieId;
    }

    public void setCategorieId(Long categorieId) {
        this.categorieId = categorieId;
    }

    public CategorieDTO getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieDTO categorie) {
        this.categorie = categorie;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UniteMesure getUniteMesureStock() {
        return uniteMesureStock;
    }

    public void setUniteMesureStock(UniteMesure uniteMesureStock) {
        this.uniteMesureStock = uniteMesureStock;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public BigDecimal getPrixVente() {
        return prixVente;
    }

    public void setPrixVente(BigDecimal prixVente) {
        this.prixVente = prixVente;
    }

    public ProduitImageDTO getImage() {
        return image;
    }

    public void setImage(ProduitImageDTO image) {
        this.image = image;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public String getGroupeArticle() {
        return groupeArticle;
    }

    public void setGroupeArticle(String groupeArticle) {
        this.groupeArticle = groupeArticle;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getCategorieArticle() {
        return categorieArticle;
    }

    public void setCategorieArticle(String categorieArticle) {
        this.categorieArticle = categorieArticle;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public BigDecimal getPrixAchatHt() { return prixAchatHt; }
    public void setPrixAchatHt(BigDecimal prixAchatHt) { this.prixAchatHt = prixAchatHt; }

    public BigDecimal getPrixAchatTtc() { return prixAchatTtc; }
    public void setPrixAchatTtc(BigDecimal prixAchatTtc) { this.prixAchatTtc = prixAchatTtc; }

    public BigDecimal getPrixVenteHt() { return prixVenteHt; }
    public void setPrixVenteHt(BigDecimal prixVenteHt) { this.prixVenteHt = prixVenteHt; }

    public BigDecimal getPrixVenteTtc() { return prixVenteTtc; }
    public void setPrixVenteTtc(BigDecimal prixVenteTtc) { this.prixVenteTtc = prixVenteTtc; }
}
