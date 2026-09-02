package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "lignes_avoir")
public class LigneAvoir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_id", nullable = false)
    @JsonIgnore
    private Avoir avoir;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantite = BigDecimal.ONE;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = BigDecimal.valueOf(20);

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "remettre_en_stock")
    private Boolean remettreEnStock = true;

    private String motif;

    public LigneAvoir() {
    }

    public void calculerMontants() {
        if (quantite == null) quantite = BigDecimal.ZERO;
        if (prixUnitaireHT == null) prixUnitaireHT = BigDecimal.ZERO;
        if (tauxTVA == null) tauxTVA = BigDecimal.ZERO;

        this.montantHT = prixUnitaireHT.multiply(quantite).setScale(2, RoundingMode.HALF_UP);
        this.montantTVA = this.montantHT.multiply(tauxTVA).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.montantTTC = this.montantHT.add(this.montantTVA).setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Avoir getAvoir() {
        return avoir;
    }

    public void setAvoir(Avoir avoir) {
        this.avoir = avoir;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaireHT() {
        return prixUnitaireHT;
    }

    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) {
        this.prixUnitaireHT = prixUnitaireHT;
    }

    public BigDecimal getTauxTVA() {
        return tauxTVA;
    }

    public void setTauxTVA(BigDecimal tauxTVA) {
        this.tauxTVA = tauxTVA;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public Boolean getRemettreEnStock() {
        return remettreEnStock;
    }

    public void setRemettreEnStock(Boolean remettreEnStock) {
        this.remettreEnStock = remettreEnStock;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }
}
