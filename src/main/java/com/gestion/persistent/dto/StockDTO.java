package com.gestion.persistent.dto;

import com.gestion.persistent.enums.QualiteProduit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitReference;
    private String produitCodeBarre;
    private String produitDescription;
    private Long categorieId;
    private String categorieNom;
    private BigDecimal prixVente;
    private BigDecimal prixVenteTtc;
    private BigDecimal prixAchatHt;
    private String uniteMesure;
    private String statutStock; // "OK", "ALERTE", "RUPTURE"
    private BigDecimal quantiteDisponible = BigDecimal.ZERO;
    private BigDecimal quantiteReservee = BigDecimal.ZERO;
    private BigDecimal quantiteTotale = BigDecimal.ZERO;
    private BigDecimal seuilAlerte = BigDecimal.ZERO;
    private LocalDateTime derniereMaj;
    private List<StockQualiteDTO> stocksQualite = new ArrayList<>();

    public StockDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitCodeBarre() { return produitCodeBarre; }
    public void setProduitCodeBarre(String produitCodeBarre) { this.produitCodeBarre = produitCodeBarre; }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }

    public BigDecimal getPrixVenteTtc() { return prixVenteTtc; }
    public void setPrixVenteTtc(BigDecimal prixVenteTtc) { this.prixVenteTtc = prixVenteTtc; }

    public BigDecimal getPrixAchatHt() { return prixAchatHt; }
    public void setPrixAchatHt(BigDecimal prixAchatHt) { this.prixAchatHt = prixAchatHt; }

    public String getUniteMesure() { return uniteMesure; }
    public void setUniteMesure(String uniteMesure) { this.uniteMesure = uniteMesure; }

    public String getStatutStock() { return statutStock; }
    public void setStatutStock(String statutStock) { this.statutStock = statutStock; }

    public String getProduitDescription() { return produitDescription; }
    public void setProduitDescription(String produitDescription) { this.produitDescription = produitDescription; }

    public BigDecimal getQuantiteDisponible() {
        if (quantiteDisponible != null && quantiteDisponible.compareTo(BigDecimal.ZERO) != 0) {
            return quantiteDisponible;
        }
        if (stocksQualite != null && !stocksQualite.isEmpty()) {
            return stocksQualite.stream()
                    .map(StockQualiteDTO::getQuantiteDisponible)
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
                    .map(StockQualiteDTO::getQuantiteReservee)
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

    public void setQuantiteTotale(BigDecimal quantiteTotale) {
        this.quantiteTotale = quantiteTotale;
    }

    public BigDecimal getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(BigDecimal seuilAlerte) { this.seuilAlerte = seuilAlerte; }

    public LocalDateTime getDerniereMaj() { return derniereMaj; }
    public void setDerniereMaj(LocalDateTime derniereMaj) { this.derniereMaj = derniereMaj; }

    public List<StockQualiteDTO> getStocksQualite() { return stocksQualite; }
    public void setStocksQualite(List<StockQualiteDTO> stocksQualite) { this.stocksQualite = stocksQualite; }

    public BigDecimal getQuantiteTotaleDisponible() {
        return getQuantiteDisponible();
    }

    public BigDecimal getQuantiteTotaleReservee() {
        return getQuantiteReservee();
    }

    public StockQualiteDTO getStockByQualite(QualiteProduit qualite) {
        if (stocksQualite == null || stocksQualite.isEmpty()) {
            StockQualiteDTO sq = new StockQualiteDTO();
            sq.setProduitId(this.produitId);
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
}
