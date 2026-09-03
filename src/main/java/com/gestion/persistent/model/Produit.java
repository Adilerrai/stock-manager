package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.UniteMesure;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "produits")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "groupe_article")
    private String groupeArticle;

    @Column(name = "code_barre")
    private String codeBarre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @Column(name = "categorie_article")
    private String categorieArticle;

    @JdbcTypeCode(SqlTypes.JSON)
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

    @Column(name = "stock_minimum")
    private BigDecimal stockMinimum = BigDecimal.ZERO;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    public Produit() {
    }

    public BigDecimal getStockMinimum() {
        return stockMinimum != null ? stockMinimum : BigDecimal.ZERO;
    }

    public void setStockMinimum(BigDecimal stockMinimum) {
        this.stockMinimum = stockMinimum;
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

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
        if (categorie != null) {
            this.categorieArticle = categorie.getNom();
        }
    }

    public String getCategorieArticle() {
        return categorieArticle;
    }

    public void setCategorieArticle(String categorieArticle) {
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
        if (val instanceof String) {
            try {
                return Integer.valueOf((String) val);
            } catch (NumberFormatException e) {
                return null;
            }
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

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (description == null || description.trim().isEmpty()) {
            description = designation != null ? designation : "Sans description";
        }
        if (pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            pointDeVenteId = tenant != null ? tenant : 1L;
        }
    }

    public String getNom() {
        return designation != null ? designation : description;
    }

    public void setNom(String nom) {
        this.designation = nom;
    }

    public BigDecimal getPrixVenteHT() {
        return getPrixVenteHt();
    }

    public BigDecimal getPrixVenteTTC() {
        return getPrixVenteTtc();
    }

    public BigDecimal getPrixAchatHT() {
        return getPrixAchatHt();
    }

    public BigDecimal getPrixAchatTTC() {
        return getPrixAchatTtc();
    }
}
