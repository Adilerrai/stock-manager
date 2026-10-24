package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Part d'une charge d'approche affectée à une ligne de commande.
 */
@Entity
@Table(name = "allocations_charge")
public class AllocationCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_id", nullable = false)
    private ChargeAchat charge;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id", nullable = false)
    private LigneCommande ligneCommande;

    @Column(name = "base_repartition", precision = 18, scale = 4)
    private BigDecimal baseRepartition = BigDecimal.ZERO;

    @Column(precision = 9, scale = 6)
    private BigDecimal pourcentage = BigDecimal.ZERO;

    @Column(name = "montant_mad", precision = 15, scale = 2)
    private BigDecimal montantMad = BigDecimal.ZERO;

    public AllocationCharge() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ChargeAchat getCharge() { return charge; }
    public void setCharge(ChargeAchat charge) { this.charge = charge; }

    public LigneCommande getLigneCommande() { return ligneCommande; }
    public void setLigneCommande(LigneCommande ligneCommande) { this.ligneCommande = ligneCommande; }

    public BigDecimal getBaseRepartition() { return baseRepartition; }
    public void setBaseRepartition(BigDecimal baseRepartition) { this.baseRepartition = baseRepartition; }

    public BigDecimal getPourcentage() { return pourcentage; }
    public void setPourcentage(BigDecimal pourcentage) { this.pourcentage = pourcentage; }

    public BigDecimal getMontantMad() { return montantMad; }
    public void setMontantMad(BigDecimal montantMad) { this.montantMad = montantMad; }
}
