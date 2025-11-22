package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_livraison")
public class LigneLivraison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livraison_id", nullable = false)
    private Livraison livraison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    private Long quantiteLivree;

    @OneToOne(fetch = FetchType.LAZY)
    private Produit produit;

    private BigDecimal prixProduit;
    @Enumerated(EnumType.STRING)
    private QualiteProduit qualiteProduit;


    // Getters and Setters
    public QualiteProduit getQualiteProduit() {
        return qualiteProduit;
    }

    public void setQualiteProduit(QualiteProduit qualiteProduit) {
        this.qualiteProduit = qualiteProduit;
    }

    // Keep all other methods as they are
    public LigneLivraison() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Livraison getLivraison() { return livraison; }
    public void setLivraison(Livraison livraison) { this.livraison = livraison; }

    public Long getQuantiteLivree() {
        return quantiteLivree;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Depot getDepot() { return depot; }
    public void setDepot(Depot depot) { this.depot = depot; }

    public BigDecimal getPrixProduit() {
        return prixProduit;
    }

    public void setPrixProduit(BigDecimal prixProduit) {
        this.prixProduit = prixProduit;
    }

    public void setQuantiteLivree(Long quantiteLivree) {
        this.quantiteLivree = quantiteLivree;
    }

}
