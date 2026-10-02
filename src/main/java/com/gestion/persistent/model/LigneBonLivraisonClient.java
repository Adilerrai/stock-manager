package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_bon_livraison_client")
public class LigneBonLivraisonClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_livraison_client_id", nullable = false)
    @JsonIgnore
    private BonLivraisonClient bonLivraisonClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_livree", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteLivree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id")
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id")
    private Lot lot;

    @Column(name = "prix_vente", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixVente;

    @Column(name = "prix_vente_brut", precision = 15, scale = 2)
    private BigDecimal prixVenteBrut;

    @Column(name = "remise_pourcentage", precision = 5, scale = 2)
    private BigDecimal remisePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_montant", precision = 12, scale = 2)
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    @Column(name = "montant_ligne", precision = 15, scale = 2)
    private BigDecimal montantLigne;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.prixVenteBrut == null) {
            this.prixVenteBrut = this.prixVente != null ? this.prixVente : BigDecimal.ZERO;
        }
        calculerMontantLigne();
    }

    public void calculerMontantLigne() {
        BigDecimal qte = this.quantiteLivree != null ? this.quantiteLivree : BigDecimal.ZERO;
        BigDecimal brutUnit = this.prixVenteBrut != null ? this.prixVenteBrut : (this.prixVente != null ? this.prixVente : BigDecimal.ZERO);
        BigDecimal totalBrut = qte.multiply(brutUnit);

        BigDecimal montantApresRemise = totalBrut;
        if (this.remisePourcentage != null && this.remisePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reduction = totalBrut.multiply(this.remisePourcentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            montantApresRemise = totalBrut.subtract(reduction);
        }
        if (this.remiseMontant != null && this.remiseMontant.compareTo(BigDecimal.ZERO) > 0) {
            montantApresRemise = montantApresRemise.subtract(this.remiseMontant);
        }

        this.montantLigne = montantApresRemise.compareTo(BigDecimal.ZERO) >= 0 ? montantApresRemise : BigDecimal.ZERO;

        // Prix net unitaire effectif
        if (qte.compareTo(BigDecimal.ZERO) > 0) {
            this.prixVente = this.montantLigne.divide(qte, 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.prixVente = brutUnit;
        }
    }

    public LigneBonLivraisonClient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BonLivraisonClient getBonLivraisonClient() { return bonLivraisonClient; }
    public void setBonLivraisonClient(BonLivraisonClient bonLivraisonClient) { this.bonLivraisonClient = bonLivraisonClient; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantiteLivree() { return quantiteLivree; }
    public void setQuantiteLivree(BigDecimal quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public Lot getLot() { return lot; }
    public void setLot(Lot lot) { this.lot = lot; }

    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }

    public BigDecimal getPrixVenteBrut() { return prixVenteBrut != null ? prixVenteBrut : prixVente; }
    public void setPrixVenteBrut(BigDecimal prixVenteBrut) { this.prixVenteBrut = prixVenteBrut; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage != null ? remisePourcentage : BigDecimal.ZERO; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getRemiseMontant() { return remiseMontant != null ? remiseMontant : BigDecimal.ZERO; }
    public void setRemiseMontant(BigDecimal remiseMontant) { this.remiseMontant = remiseMontant; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }
}

