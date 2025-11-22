package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lots")
public class Lot {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "numero_lot", unique = true, nullable = false)
    private String numeroLot;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "qualite", nullable = false)
    private QualiteProduit qualite;
    
    @Column(name = "quantite_initiale", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantiteInitiale;
    
    @Column(name = "quantite_disponible", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantiteDisponible;
    
    @Column(name = "quantite_reservee", precision = 10, scale = 2)
    private BigDecimal quantiteReservee = BigDecimal.ZERO;
    
    @Column(name = "date_fabrication")
    private LocalDate dateFabrication;
    
    @Column(name = "date_expiration")
    private LocalDate dateExpiration;
    
    @Column(name = "date_reception", nullable = false)
    private LocalDateTime dateReception;
    
    @Column(name = "prix_achat_unitaire", precision = 10, scale = 2)
    private BigDecimal prixAchatUnitaire;
    
    @Column(name = "numero_livraison")
    private String numeroLivraison;
    
    @Column(name = "fournisseur")
    private String fournisseur;
    
    @Column(name = "observations")
    private String observations;
    
    @Column(name = "actif")
    private Boolean actif = true;

    // Constructeurs, getters et setters
    public Lot() {}
    
    public String generateNumeroLot() {
        return "LOT-" + System.currentTimeMillis();
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(String fournisseur) {
        this.fournisseur = fournisseur;
    }

    public String getNumeroLivraison() {
        return numeroLivraison;
    }

    public void setNumeroLivraison(String numeroLivraison) {
        this.numeroLivraison = numeroLivraison;
    }

    public BigDecimal getPrixAchatUnitaire() {
        return prixAchatUnitaire;
    }

    public void setPrixAchatUnitaire(BigDecimal prixAchatUnitaire) {
        this.prixAchatUnitaire = prixAchatUnitaire;
    }

    public LocalDateTime getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDateTime dateReception) {
        this.dateReception = dateReception;
    }

    public LocalDate getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDate dateExpiration) {
        this.dateExpiration = dateExpiration;
    }

    public LocalDate getDateFabrication() {
        return dateFabrication;
    }

    public void setDateFabrication(LocalDate dateFabrication) {
        this.dateFabrication = dateFabrication;
    }

    public BigDecimal getQuantiteReservee() {
        return quantiteReservee;
    }

    public void setQuantiteReservee(BigDecimal quantiteReservee) {
        this.quantiteReservee = quantiteReservee;
    }

    public BigDecimal getQuantiteDisponible() {
        return quantiteDisponible;
    }

    public void setQuantiteDisponible(BigDecimal quantiteDisponible) {
        this.quantiteDisponible = quantiteDisponible;
    }

    public BigDecimal getQuantiteInitiale() {
        return quantiteInitiale;
    }

    public void setQuantiteInitiale(BigDecimal quantiteInitiale) {
        this.quantiteInitiale = quantiteInitiale;
    }

    public QualiteProduit getQualite() {
        return qualite;
    }

    public void setQualite(QualiteProduit qualite) {
        this.qualite = qualite;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public String getNumeroLot() {
        return numeroLot;
    }

    public void setNumeroLot(String numeroLot) {
        this.numeroLot = numeroLot;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}