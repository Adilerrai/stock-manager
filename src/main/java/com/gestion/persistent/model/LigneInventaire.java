package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "lignes_inventaire")
public class LigneInventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventaire_id", nullable = false)
    @JsonIgnore
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite")
    private QualiteProduit qualite = QualiteProduit.PREMIERE_QUALITE;

    @Column(name = "quantite_theorique", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantiteTheorique = BigDecimal.ZERO;

    @Column(name = "quantite_reelle", precision = 12, scale = 3)
    private BigDecimal quantiteReelle = BigDecimal.ZERO;

    @Column(precision = 12, scale = 3)
    private BigDecimal ecart = BigDecimal.ZERO;

    @Column(name = "prix_unitaire", precision = 15, scale = 2)
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    @Column(name = "valeur_ecart", precision = 15, scale = 2)
    private BigDecimal valeurEcart = BigDecimal.ZERO;

    public LigneInventaire() {
    }

    public void calculerEcart() {
        if (quantiteTheorique == null) quantiteTheorique = BigDecimal.ZERO;
        if (quantiteReelle == null) quantiteReelle = BigDecimal.ZERO;
        if (prixUnitaire == null) prixUnitaire = BigDecimal.ZERO;

        this.ecart = quantiteReelle.subtract(quantiteTheorique);
        this.valeurEcart = this.ecart.multiply(prixUnitaire).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Inventaire getInventaire() {
        return inventaire;
    }

    public void setInventaire(Inventaire inventaire) {
        this.inventaire = inventaire;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public QualiteProduit getQualite() {
        return qualite;
    }

    public void setQualite(QualiteProduit qualite) {
        this.qualite = qualite;
    }

    public BigDecimal getQuantiteTheorique() {
        return quantiteTheorique;
    }

    public void setQuantiteTheorique(BigDecimal quantiteTheorique) {
        this.quantiteTheorique = quantiteTheorique;
    }

    public BigDecimal getQuantiteReelle() {
        return quantiteReelle;
    }

    public void setQuantiteReelle(BigDecimal quantiteReelle) {
        this.quantiteReelle = quantiteReelle;
    }

    public BigDecimal getEcart() {
        return ecart;
    }

    public void setEcart(BigDecimal ecart) {
        this.ecart = ecart;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getValeurEcart() {
        return valeurEcart;
    }

    public void setValeurEcart(BigDecimal valeurEcart) {
        this.valeurEcart = valeurEcart;
    }
}
