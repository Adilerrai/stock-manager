package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @OneToMany(mappedBy = "stock", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StockQualite> stocksQualite = new ArrayList<>();

    // Getter et setter
    public List<StockQualite> getStocksQualite() { return stocksQualite; }
    public void setStocksQualite(List<StockQualite> stocksQualite) { this.stocksQualite = stocksQualite; }

    // Méthode utilitaire pour obtenir le stock total toutes qualités confondues
    public BigDecimal getQuantiteTotaleDisponible() {
        return stocksQualite.stream()
                .map(StockQualite::getQuantiteDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getQuantiteTotaleReservee() {
        return stocksQualite.stream()
                .map(StockQualite::getQuantiteReservee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Méthodes utilitaires pour gérer les qualités
    public StockQualite getStockByQualite(QualiteProduit qualite) {
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
        return stocksQualite.stream()
                .anyMatch(sq -> sq.getQualite() == qualite && 
                               sq.getQuantiteDisponible().compareTo(BigDecimal.ZERO) > 0);
    }

    // Constructors, getters, setters
    public Stock() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }
}
