package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_transfert_stock")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneTransfertStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfert_id", nullable = false)
    @JsonBackReference
    private TransfertStock transfert;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_demandee", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantiteDemandee = BigDecimal.ZERO;

    @Column(name = "quantite_expediee", precision = 12, scale = 2)
    private BigDecimal quantiteExpediee = BigDecimal.ZERO;

    @Column(name = "quantite_recue", precision = 12, scale = 2)
    private BigDecimal quantiteRecue = BigDecimal.ZERO;

    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public LigneTransfertStock() {}

    public LigneTransfertStock(Produit produit, BigDecimal quantite, String notes, Long pointDeVenteId) {
        this.produit = produit;
        this.quantiteDemandee = quantite != null ? quantite : BigDecimal.ZERO;
        this.quantiteExpediee = this.quantiteDemandee;
        this.quantiteRecue = this.quantiteDemandee;
        this.notes = notes;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
    }

    public LigneTransfertStock(Produit produit, BigDecimal quantiteDemandee, Long pointDeVenteId) {
        this(produit, quantiteDemandee, null, pointDeVenteId);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TransfertStock getTransfert() { return transfert; }
    public void setTransfert(TransfertStock transfert) { this.transfert = transfert; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public BigDecimal getQuantiteExpediee() { return quantiteExpediee; }
    public void setQuantiteExpediee(BigDecimal quantiteExpediee) { this.quantiteExpediee = quantiteExpediee; }

    public BigDecimal getQuantiteRecue() { return quantiteRecue; }
    public void setQuantiteRecue(BigDecimal quantiteRecue) { this.quantiteRecue = quantiteRecue; }

    public BigDecimal getQuantite() {
        return quantiteDemandee;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantiteDemandee = quantite != null ? quantite : BigDecimal.ZERO;
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
