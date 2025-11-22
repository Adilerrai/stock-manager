package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.UniteMesure;
import com.ceramique.persistent.enums.QualiteProduit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDTO {
    private Long id;
    private Long produitId;
    private String produitDescription;
    private List<StockQualiteDTO> stocksQualite = new ArrayList<>();

    // Constructors, getters, setters
    public StockDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitDescription() { return produitDescription; }
    public void setProduitDescription(String produitDescription) { this.produitDescription = produitDescription; }

    public List<StockQualiteDTO> getStocksQualite() { return stocksQualite; }
    public void setStocksQualite(List<StockQualiteDTO> stocksQualite) { this.stocksQualite = stocksQualite; }

    // Méthodes utilitaires
    public BigDecimal getQuantiteTotaleDisponible() {
        return stocksQualite.stream()
                .map(StockQualiteDTO::getQuantiteDisponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getQuantiteTotaleReservee() {
        return stocksQualite.stream()
                .map(StockQualiteDTO::getQuantiteReservee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public StockQualiteDTO getStockByQualite(QualiteProduit qualite) {
        return stocksQualite.stream()
                .filter(sq -> sq.getQualite() == qualite)
                .findFirst()
                .orElse(null);
    }
}
