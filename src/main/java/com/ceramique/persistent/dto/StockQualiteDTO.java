package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.QualiteProduit;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockQualiteDTO {
    private Long id;
    private QualiteProduit qualite;
    private BigDecimal quantiteDisponible;
    private BigDecimal quantiteReservee;
    private BigDecimal seuilAlerte;
    private LocalDateTime derniereMaj;
    private Long produitId;
    private String produitDescription;
    private String produitNom; // ajout

    // Constructors
    public StockQualiteDTO() {}

    public StockQualiteDTO(QualiteProduit qualite, BigDecimal quantiteDisponible) {
        this.qualite = qualite;
        this.quantiteDisponible = quantiteDisponible;
        this.quantiteReservee = BigDecimal.ZERO;
    }

    public StockQualiteDTO(QualiteProduit qualite, BigDecimal quantiteDisponible, BigDecimal quantiteReservee) {
        this.qualite = qualite;
        this.quantiteDisponible = quantiteDisponible;
        this.quantiteReservee = quantiteReservee;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitDescription() { return produitDescription; }
    public void setProduitDescription(String produitDescription) { this.produitDescription = produitDescription; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    // Méthode utilitaire pour calculer la quantité totale
    public BigDecimal getQuantiteTotale() {
        return quantiteDisponible.add(quantiteReservee != null ? quantiteReservee : BigDecimal.ZERO);
    }
}
