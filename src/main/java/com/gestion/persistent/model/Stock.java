package com.gestion.persistent.model;

import com.gestion.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stocks", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"produit_id"})
})
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_disponible", precision = 12, scale = 2, nullable = false)
    private BigDecimal quantiteDisponible = BigDecimal.ZERO;

    @Column(name = "quantite_reservee", precision = 12, scale = 2)
    private BigDecimal quantiteReservee = BigDecimal.ZERO;

    @Column(name = "seuil_alerte", precision = 12, scale = 2)
    private BigDecimal seuilAlerte = BigDecimal.ZERO;

    @Column(name = "derniere_maj")
    private LocalDateTime derniereMaj = LocalDateTime.now();

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StockQualite> stocksQualite = new ArrayList<>();

    public Stock() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantiteDisponible() {
        if (quantiteDisponible != null && quantiteDisponible.compareTo(BigDecimal.ZERO) != 0) {
            return quantiteDisponible;
        }
        if (stocksQualite != null && !stocksQualite.isEmpty()) {
            return stocksQualite.stream()
                    .map(StockQualite::getQuantiteDisponible)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return quantiteDisponible != null ? quantiteDisponible : BigDecimal.ZERO;
    }

    public void setQuantiteDisponible(BigDecimal quantiteDisponible) {
        this.quantiteDisponible = quantiteDisponible != null ? quantiteDisponible : BigDecimal.ZERO;
    }

    public BigDecimal getQuantiteReservee() {
        if (quantiteReservee != null && quantiteReservee.compareTo(BigDecimal.ZERO) != 0) {
            return quantiteReservee;
        }
        if (stocksQualite != null && !stocksQualite.isEmpty()) {
            return stocksQualite.stream()
                    .map(StockQualite::getQuantiteReservee)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return quantiteReservee != null ? quantiteReservee : BigDecimal.ZERO;
    }

    public void setQuantiteReservee(BigDecimal quantiteReservee) {
        this.quantiteReservee = quantiteReservee != null ? quantiteReservee : BigDecimal.ZERO;
    }

    public BigDecimal getQuantiteTotale() {
        return getQuantiteDisponible().add(getQuantiteReservee());
    }

    public BigDecimal getSeuilAlerte() {
        return seuilAlerte != null ? seuilAlerte : BigDecimal.ZERO;
    }

    public void setSeuilAlerte(BigDecimal seuilAlerte) {
        this.seuilAlerte = seuilAlerte != null ? seuilAlerte : BigDecimal.ZERO;
    }

    public LocalDateTime getDerniereMaj() { return derniereMaj; }
    public void setDerniereMaj(LocalDateTime derniereMaj) { this.derniereMaj = derniereMaj; }

    // Compatibilité
    public BigDecimal getQuantiteTotaleDisponible() {
        return getQuantiteDisponible();
    }

    public BigDecimal getQuantiteTotaleReservee() {
        return getQuantiteReservee();
    }

    public List<StockQualite> getStocksQualite() { return stocksQualite; }
    public void setStocksQualite(List<StockQualite> stocksQualite) { this.stocksQualite = stocksQualite; }

    public StockQualite getStockByQualite(QualiteProduit qualite) {
        if (stocksQualite == null || stocksQualite.isEmpty()) {
            StockQualite sq = new StockQualite();
            sq.setStock(this);
            sq.setProduit(this.produit);
            sq.setQualite(qualite != null ? qualite : QualiteProduit.PREMIERE_QUALITE);
            sq.setQuantiteDisponible(getQuantiteDisponible());
            sq.setQuantiteReservee(getQuantiteReservee());
            sq.setSeuilAlerte(getSeuilAlerte());
            return sq;
        }
        return stocksQualite.stream()
                .filter(sq -> sq.getQualite() == qualite)
                .findFirst()
                .orElse(null);
    }

    public void ajouterStockQualite(StockQualite stockQualite) {
        stockQualite.setStock(this);
        this.stocksQualite.add(stockQualite);
    }

    public boolean hasStockForQualite(QualiteProduit qualite) {
        return getQuantiteDisponible().compareTo(BigDecimal.ZERO) > 0;
    }
}
