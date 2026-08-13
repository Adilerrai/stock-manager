package com.ceramique.persistent.model;

import com.ceramique.persistent.converter.JsonToMapConverter;
import com.ceramique.persistent.enums.UniteMesure;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "produits")
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    private String designation;

    @Column(nullable = false)
    private String description;

    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    private GroupeArticle groupeArticle;

    @Column(name = "code_barre")
    private String codeBarre;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_article")
    private com.ceramique.persistent.enums.CategorieArticle categorieArticle;

    @Convert(converter = JsonToMapConverter.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(name = "prix_achat_ht")
    private BigDecimal prixAchatHt;

    @Column(name = "prix_achat_ttc")
    private BigDecimal prixAchatTtc;

    @Column(name = "prix_vente_ht")
    private BigDecimal prixVenteHt;

    @Column(name = "prix_vente_ttc")
    private BigDecimal prixVenteTtc;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "produit")
    private ProduitImage image;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    private BigDecimal prixAchat;

    private BigDecimal prixVente;

    @Enumerated(EnumType.STRING)
    @Column(name = "unite_mesure_stock")
    private UniteMesure uniteMesureStock;

    public Produit() {
    }

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

    public ProduitImage getImage() {
        return image;
    }

    public void setImage(ProduitImage image) {
        this.image = image;
        if (image != null) {
            image.setProduit(this);
        }
    }

    public BigDecimal getLongueurCm() {
        return getAttributeAsBigDecimal("longueurCm");
    }

    public void setLongueurCm(BigDecimal longueurCm) {
        setAttribute("longueurCm", longueurCm);
    }

    public BigDecimal getLargeurCm() {
        return getAttributeAsBigDecimal("largeurCm");
    }

    public void setLargeurCm(BigDecimal largeurCm) {
        setAttribute("largeurCm", largeurCm);
    }

    public BigDecimal getEpaisseurMm() {
        return getAttributeAsBigDecimal("epaisseurMm");
    }

    public void setEpaisseurMm(BigDecimal epaisseurMm) {
        setAttribute("epaisseurMm", epaisseurMm);
    }

    public String getFormat() {
        return getAttributeAsString("format");
    }

    public void setFormat(String format) {
        setAttribute("format", format);
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
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

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getCouleur() {
        return getAttributeAsString("couleur");
    }

    public void setCouleur(String couleur) {
        setAttribute("couleur", couleur);
    }

    public String getTexture() {
        return getAttributeAsString("texture");
    }

    public void setTexture(String texture) {
        setAttribute("texture", texture);
    }

    public String getFinition() {
        return getAttributeAsString("finition");
    }

    public void setFinition(String finition) {
        setAttribute("finition", finition);
    }

    public String getOrigine() {
        return getAttributeAsString("origine");
    }

    public void setOrigine(String origine) {
        setAttribute("origine", origine);
    }

    public String getSerie() {
        return getAttributeAsString("serie");
    }

    public void setSerie(String serie) {
        setAttribute("serie", serie);
    }

    public BigDecimal getSurfaceParBoiteM2() {
        return getAttributeAsBigDecimal("surfaceParBoiteM2");
    }

    public void setSurfaceParBoiteM2(BigDecimal surfaceParBoiteM2) {
        setAttribute("surfaceParBoiteM2", surfaceParBoiteM2);
    }

    public Integer getQuantiteParBoite() {
        return getAttributeAsInteger("quantiteParBoite");
    }

    public void setQuantiteParBoite(Integer quantiteParBoite) {
        setAttribute("quantiteParBoite", quantiteParBoite);
    }

    public com.ceramique.persistent.enums.CategorieArticle getCategorieArticle() {
        return categorieArticle;
    }

    public void setCategorieArticle(com.ceramique.persistent.enums.CategorieArticle categorieArticle) {
        this.categorieArticle = categorieArticle;
    }

    // Dynamic attributes
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    // Pricing Fields Getters & Setters
    public BigDecimal getPrixAchatHt() {
        return prixAchatHt;
    }

    public void setPrixAchatHt(BigDecimal prixAchatHt) {
        this.prixAchatHt = prixAchatHt;
    }

    public BigDecimal getPrixAchatTtc() {
        return prixAtcFallback(prixAchatTtc, prixAchat);
    }

    public void setPrixAchatTtc(BigDecimal prixAchatTtc) {
        this.prixAchatTtc = prixAchatTtc;
        this.prixAchat = prixAchatTtc;
    }

    public BigDecimal getPrixVenteHt() {
        return prixVenteHt;
    }

    public void setPrixVenteHt(BigDecimal prixVenteHt) {
        this.prixVenteHt = prixVenteHt;
    }

    public BigDecimal getPrixVenteTtc() {
        return prixAtcFallback(prixVenteTtc, prixVente);
    }

    public void setPrixVenteTtc(BigDecimal prixVenteTtc) {
        this.prixVenteTtc = prixVenteTtc;
        this.prixVente = prixVenteTtc;
    }

    public BigDecimal getPpvHt() {
        return ppvHt;
    }

    public void setPpvHt(BigDecimal ppvHt) {
        this.ppvHt = ppvHt;
    }

    public BigDecimal getPpvTtc() {
        return ppvTtc;
    }

    public void setPpvTtc(BigDecimal ppvTtc) {
        this.ppvTtc = ppvTtc;
    }

    public BigDecimal getPphHt() {
        return pphHt;
    }

    public void setPphHt(BigDecimal pphHt) {
        this.pphHt = pphHt;
    }

    public BigDecimal getPphTtc() {
        return pphTtc;
    }

    public void setPphTtc(BigDecimal pphTtc) {
        this.pphTtc = pphTtc;
    }

    private BigDecimal prixAtcFallback(BigDecimal newPrice, BigDecimal oldPrice) {
        if (newPrice != null)
            return newPrice;
        return oldPrice;
    }

    // Helpers EAV
    private void setAttribute(String key, Object value) {
        if (this.attributes == null) {
            this.attributes = new HashMap<>();
        }
        if (value == null) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
    }

    private String getAttributeAsString(String key) {
        return this.attributes != null && this.attributes.containsKey(key) ? (String) this.attributes.get(key) : null;
    }

    private Integer getAttributeAsInteger(String key) {
        if (this.attributes == null || !this.attributes.containsKey(key)) {
            return null;
        }
        Object val = this.attributes.get(key);
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        return null;
    }

    private BigDecimal getAttributeAsBigDecimal(String key) {
        if (this.attributes == null || !this.attributes.containsKey(key)) {
            return null;
        }
        Object val = this.attributes.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        if (val instanceof String) {
            try {
                return new BigDecimal((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
