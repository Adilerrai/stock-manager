package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_facture_achat")
public class LigneFactureAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_achat_id", nullable = false)
    @JsonIgnore
    private FactureAchat factureAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 15, scale = 2)
    private BigDecimal prixUnitaireHt;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTva = BigDecimal.valueOf(19.00);

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHt;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTva;

    @Column(name = "montant_ttc", precision = 15, scale = 2)
    private BigDecimal montantTtc;

    public LigneFactureAchat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public FactureAchat getFactureAchat() { return factureAchat; }
    public void setFactureAchat(FactureAchat factureAchat) { this.factureAchat = factureAchat; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaireHt() { return prixUnitaireHt; }
    public void setPrixUnitaireHt(BigDecimal prixUnitaireHt) { this.prixUnitaireHt = prixUnitaireHt; }

    public BigDecimal getTauxTva() { return tauxTva; }
    public void setTauxTva(BigDecimal tauxTva) { this.tauxTva = tauxTva; }

    public BigDecimal getMontantHt() { return montantHt; }
    public void setMontantHt(BigDecimal montantHt) { this.montantHt = montantHt; }

    public BigDecimal getMontantTva() { return montantTva; }
    public void setMontantTva(BigDecimal montantTva) { this.montantTva = montantTva; }

    public BigDecimal getMontantTtc() { return montantTtc; }
    public void setMontantTtc(BigDecimal montantTtc) { this.montantTtc = montantTtc; }
}

