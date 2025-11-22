package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.persistent.enums.TypeMouvement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_stock")
public class MouvementStock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false)
    private TypeMouvement typeMouvement;
    
    @Column(name = "quantite", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantite;
    
    @Column(name = "quantite_avant", precision = 10, scale = 2)
    private BigDecimal quantiteAvant;
    
    @Column(name = "quantite_apres", precision = 10, scale = 2)
    private BigDecimal quantiteApres;
    
    @Column(name = "reference_document")
    private String referenceDocument;
    
    @Column(name = "motif")
    private String motif;
    
    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;
    
    @Column(name = "utilisateur")
    private String utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_produit")
    private QualiteProduit qualiteProduit;

    @Column(name = "numero_lot_externe")
    private String numeroLotExterne; // Pour les lots fournisseurs

    // NEW: depot relation (colonne depot_id is NOT NULL in DB)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    // Getters et setters pour les nouveaux champs
    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }

    public QualiteProduit getQualiteProduit() { return qualiteProduit; }
    public void setQualiteProduit(QualiteProduit qualiteProduit) { this.qualiteProduit = qualiteProduit; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    // Constructeurs, getters et setters
    public MouvementStock() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }


    public TypeMouvement getTypeMouvement() {
        return typeMouvement;
    }

    public void setTypeMouvement(TypeMouvement typeMouvement) {
        this.typeMouvement = typeMouvement;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getQuantiteAvant() {
        return quantiteAvant;
    }

    public void setQuantiteAvant(BigDecimal quantiteAvant) {
        this.quantiteAvant = quantiteAvant;
    }

    public BigDecimal getQuantiteApres() {
        return quantiteApres;
    }

    public void setQuantiteApres(BigDecimal quantiteApres) {
        this.quantiteApres = quantiteApres;
    }

    public String getReferenceDocument() {
        return referenceDocument;
    }

    public void setReferenceDocument(String referenceDocument) {
        this.referenceDocument = referenceDocument;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateMouvement() {
        return dateMouvement;
    }

    public void setDateMouvement(LocalDateTime dateMouvement) {
        this.dateMouvement = dateMouvement;
    }

    public String getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getNumeroLotExterne() {
        return numeroLotExterne;
    }

    public void setNumeroLotExterne(String numeroLotExterne) {
        this.numeroLotExterne = numeroLotExterne;
    }
}