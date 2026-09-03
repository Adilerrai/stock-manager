package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_bon_preparation")
public class LigneBonPreparation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bon_preparation_id", nullable = false)
    @JsonIgnore
    private BonPreparation bonPreparation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_commandee", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantiteCommandee = BigDecimal.ZERO;

    @Column(name = "quantite_preparee", precision = 12, scale = 3, nullable = false)
    private BigDecimal quantitePreparee = BigDecimal.ZERO;

    @Column(name = "emplacement_depot")
    private String emplacementDepot;

    @Column(name = "statut_ligne")
    private String statutLigne = "EN_ATTENTE"; // EN_ATTENTE, PREPARE, MANQUANT, PARTIEL

    public LigneBonPreparation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BonPreparation getBonPreparation() { return bonPreparation; }
    public void setBonPreparation(BonPreparation bonPreparation) { this.bonPreparation = bonPreparation; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(BigDecimal quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public BigDecimal getQuantitePreparee() { return quantitePreparee; }
    public void setQuantitePreparee(BigDecimal quantitePreparee) { this.quantitePreparee = quantitePreparee; }

    public String getEmplacementDepot() { return emplacementDepot; }
    public void setEmplacementDepot(String emplacementDepot) { this.emplacementDepot = emplacementDepot; }

    public String getStatutLigne() { return statutLigne; }
    public void setStatutLigne(String statutLigne) { this.statutLigne = statutLigne; }
}
