package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.UniteMesure;
import com.ceramique.persistent.enums.CategorieArticle;
import com.ceramique.persistent.model.GroupeArticle;

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
    private GroupeArticle groupeArticle;
    
    private BigDecimal longueurCm;
    private BigDecimal largeurCm;
    private BigDecimal epaisseurMm;
    private String format;
    private String codeBarre;
    private String couleur;
    private String texture;
    private String finition;
    private String origine;
    private String serie;
    private BigDecimal surfaceParBoiteM2;
    private Integer quantiteParBoite;
    private CategorieArticle categorieArticle;
    
    private Map<String, Object> attributes;
    
    // Tarifs additionnels HT / TTC / PPV / PPH
    private BigDecimal prixAchatHt;
    private BigDecimal prixAchatTtc;
    private BigDecimal prixVenteHt;
    private BigDecimal prixVenteTtc;
    private BigDecimal ppvHt;
    private BigDecimal ppvTtc;
    private BigDecimal pphHt;
    private BigDecimal pphTtc;

    // Constructors
    public ProduitDTO() {
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

    public GroupeArticle getGroupeArticle() {
        return groupeArticle;
    }

    public void setGroupeArticle(GroupeArticle groupeArticle) {
        this.groupeArticle = groupeArticle;
    }

    public BigDecimal getLongueurCm() {
        return longueurCm;
    }

    public void setLongueurCm(BigDecimal longueurCm) {
        this.longueurCm = longueurCm;
    }

    public BigDecimal getLargeurCm() {
        return largeurCm;
    }

    public void setLargeurCm(BigDecimal largeurCm) {
        this.largeurCm = largeurCm;
    }

    public BigDecimal getEpaisseurMm() {
        return epaisseurMm;
    }

    public void setEpaisseurMm(BigDecimal epaisseurMm) {
        this.epaisseurMm = epaisseurMm;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public String getFinition() {
        return finition;
    }

    public void setFinition(String finition) {
        this.finition = finition;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public String getSerie() {
        return serie;
    }

    public void setSerie(String serie) {
        this.serie = serie;
    }

    public BigDecimal getSurfaceParBoiteM2() {
        return surfaceParBoiteM2;
    }

    public void setSurfaceParBoiteM2(BigDecimal surfaceParBoiteM2) {
        this.surfaceParBoiteM2 = surfaceParBoiteM2;
    }

    public Integer getQuantiteParBoite() {
        return quantiteParBoite;
    }

    public void setQuantiteParBoite(Integer quantiteParBoite) {
        this.quantiteParBoite = quantiteParBoite;
    }

    public CategorieArticle getCategorieArticle() {
        return categorieArticle;
    }

    public void setCategorieArticle(CategorieArticle categorieArticle) {
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

    public BigDecimal getPpvHt() { return ppvHt; }
    public void setPpvHt(BigDecimal ppvHt) { this.ppvHt = ppvHt; }

    public BigDecimal getPpvTtc() { return ppvTtc; }
    public void setPpvTtc(BigDecimal ppvTtc) { this.ppvTtc = ppvTtc; }

    public BigDecimal getPphHt() { return pphHt; }
    public void setPphHt(BigDecimal pphHt) { this.pphHt = pphHt; }

    public BigDecimal getPphTtc() { return pphTtc; }
    public void setPphTtc(BigDecimal pphTtc) { this.pphTtc = pphTtc; }
}