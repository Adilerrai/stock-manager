package com.ceramique.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_vente")
public class LigneVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vente_id", nullable = false)
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    private String designation;

    private String reference;

    @Column(name = "quantite", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "surface_m2", precision = 15, scale = 2)
    private BigDecimal surfaceM2; // Surface en m² demandée par le client

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaireHT;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = new BigDecimal("19.00"); // TVA par défaut en Algérie

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTVA;

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "remise_pourcentage", precision = 5, scale = 2)
    private BigDecimal remisePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_montant", precision = 15, scale = 2)
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    private String notes;

    // Constructeurs
    public LigneVente() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Vente getVente() {
        return vente;
    }

    public void setVente(Vente vente) {
        this.vente = vente;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(BigDecimal surfaceM2) {
        this.surfaceM2 = surfaceM2;
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

    public BigDecimal getRemisePourcentage() {
        return remisePourcentage;
    }

    public void setRemisePourcentage(BigDecimal remisePourcentage) {
        this.remisePourcentage = remisePourcentage;
    }

    public BigDecimal getRemiseMontant() {
        return remiseMontant;
    }

    public void setRemiseMontant(BigDecimal remiseMontant) {
        this.remiseMontant = remiseMontant;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Méthodes de calcul
    public void calculerMontants() {
        // Calcul montant HT brut
        BigDecimal montantBrut = prixUnitaireHT.multiply(quantite);

        // Application de la remise
        if (remisePourcentage != null && remisePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            remiseMontant = montantBrut.multiply(remisePourcentage).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
        }

        // Montant HT après remise
        this.montantHT = montantBrut.subtract(remiseMontant != null ? remiseMontant : BigDecimal.ZERO);

        // Calcul TVA
        this.montantTVA = montantHT.multiply(tauxTVA).divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);

        // Montant TTC
        this.montantTTC = montantHT.add(montantTVA);
    }
}

