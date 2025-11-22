package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_qualites", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"stock_id", "qualite_produit"})
})
public class StockQualite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_produit", nullable = false)
    private QualiteProduit qualite;

    @Column(name = "quantite_disponible", precision = 10, scale = 2, nullable = false)
    private BigDecimal quantiteDisponible = BigDecimal.ZERO;

    @Column(name = "quantite_reservee", precision = 10, scale = 2)
    private BigDecimal quantiteReservee = BigDecimal.ZERO;

    @Column(name = "seuil_alerte", precision = 10, scale = 2)
    private BigDecimal seuilAlerte;

    @Column(name = "derniere_maj")
    private LocalDateTime derniereMaj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    // Constructors
    public StockQualite() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Depot getDepot() { return null; }


    public QualiteProduit getQualite() { return qualite; }
    public void setQualite(QualiteProduit qualite) { this.qualite = qualite; }

    public BigDecimal getQuantiteDisponible() { return quantiteDisponible; }
    public void setQuantiteDisponible(BigDecimal quantiteDisponible) { this.quantiteDisponible = quantiteDisponible; }

    public BigDecimal getQuantiteReservee() { return quantiteReservee; }
    public void setQuantiteReservee(BigDecimal quantiteReservee) { this.quantiteReservee = quantiteReservee; }

    public BigDecimal getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(BigDecimal seuilAlerte) { this.seuilAlerte = seuilAlerte; }

    public LocalDateTime getDerniereMaj() { return derniereMaj; }
    public void setDerniereMaj(LocalDateTime derniereMaj) { this.derniereMaj = derniereMaj; }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    @PrePersist
    @PreUpdate
    public void updateTimestamp() {
        this.derniereMaj = LocalDateTime.now();
    }

    // Méthodes utilitaires
    public BigDecimal getQuantiteTotale() {
        return quantiteDisponible.add(quantiteReservee != null ? quantiteReservee : BigDecimal.ZERO);
    }

    public boolean isStockFaible() {
        return seuilAlerte != null && quantiteDisponible.compareTo(seuilAlerte) <= 0;
    }

    public boolean reserverStock(BigDecimal quantite) {
        if (quantiteDisponible.compareTo(quantite) >= 0) {
            quantiteDisponible = quantiteDisponible.subtract(quantite);
            quantiteReservee = quantiteReservee.add(quantite);
            return true;
        }
        return false;
    }

    public void libererStock(BigDecimal quantite) {
        if (quantiteReservee.compareTo(quantite) >= 0) {
            quantiteReservee = quantiteReservee.subtract(quantite);
            quantiteDisponible = quantiteDisponible.add(quantite);
        }
    }
}
