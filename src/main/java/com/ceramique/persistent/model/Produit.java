package com.ceramique.persistent.model;

import com.acommon.persistant.model.PointDeVente;
import com.ceramique.persistent.enums.UniteMesure;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produits")
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String reference;

    private String designation;

    @Column(name = "longueur_cm")
    private BigDecimal longueurCm;

    @Column(name = "largeur_cm")
    private BigDecimal largeurCm;

    @Column(name = "epaisseur_mm")
    private BigDecimal epaisseurMm;

    private String format; // "60x60", "30x60"

    @Column(nullable = false)
    private String description;

    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    private GroupeArticle groupeArticle;

    // Champs spécifiques céramique
    @Column(name = "code_barre")
    private String codeBarre;

    private String couleur;

    private String texture;

    private String finition; // Mat, Brillant, Satiné

    private String origine; // Pays d'origine

    private String serie;

    @Column(name = "surface_par_boite_m2", precision = 10, scale = 4)
    private BigDecimal surfaceParBoiteM2;

    @Column(name = "quantite_par_boite")
    private Integer quantiteParBoite;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_article")
    private com.ceramique.persistent.enums.CategorieArticle categorieArticle;


    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "produit")
    private ProduitImage image;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_de_vente_id", nullable = false)
    private PointDeVente pointDeVente;

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

    public String getDesignation() {
        return designation;
    }
    public void setDesignation(String designation) {
        this.designation = designation;
    }
    public PointDeVente getPointDeVente() {
        return pointDeVente;
    }

    public void setPointDeVente(PointDeVente pointDeVente) {
        this.pointDeVente = pointDeVente;
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

    public com.ceramique.persistent.enums.CategorieArticle getCategorieArticle() {
        return categorieArticle;
    }

    public void setCategorieArticle(com.ceramique.persistent.enums.CategorieArticle categorieArticle) {
        this.categorieArticle = categorieArticle;
    }
}
