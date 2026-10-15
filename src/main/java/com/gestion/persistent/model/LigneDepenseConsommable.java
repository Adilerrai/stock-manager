package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_depenses_consommables")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneDepenseConsommable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depense_id", nullable = false)
    @JsonIgnore
    private Depense depense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consommable_id", nullable = false)
    private Consommable consommable;

    @Column(name = "quantite", precision = 12, scale = 2, nullable = false)
    private BigDecimal quantite = BigDecimal.ONE;

    @Column(name = "prix_unitaire", precision = 12, scale = 2, nullable = false)
    private BigDecimal prixUnitaire = BigDecimal.ZERO;

    @Column(name = "montant_total", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "notes")
    private String notes;

    public LigneDepenseConsommable() {}

    public LigneDepenseConsommable(Depense depense, Consommable consommable, BigDecimal quantite, BigDecimal prixUnitaire) {
        this.depense = depense;
        this.consommable = consommable;
        this.quantite = quantite != null ? quantite : BigDecimal.ONE;
        this.prixUnitaire = prixUnitaire != null ? prixUnitaire : BigDecimal.ZERO;
        this.montantTotal = this.quantite.multiply(this.prixUnitaire);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Depense getDepense() { return depense; }
    public void setDepense(Depense depense) { this.depense = depense; }

    public Consommable getConsommable() { return consommable; }
    public void setConsommable(Consommable consommable) { this.consommable = consommable; }

    public BigDecimal getQuantite() { return quantite != null ? quantite : BigDecimal.ZERO; }
    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
        calculerTotal();
    }

    public BigDecimal getPrixUnitaire() { return prixUnitaire != null ? prixUnitaire : BigDecimal.ZERO; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        calculerTotal();
    }

    public BigDecimal getMontantTotal() { return montantTotal != null ? montantTotal : BigDecimal.ZERO; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public void calculerTotal() {
        BigDecimal q = this.quantite != null ? this.quantite : BigDecimal.ZERO;
        BigDecimal pu = this.prixUnitaire != null ? this.prixUnitaire : BigDecimal.ZERO;
        this.montantTotal = q.multiply(pu);
    }
}
