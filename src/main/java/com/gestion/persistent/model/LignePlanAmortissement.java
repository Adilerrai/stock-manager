package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lignes_plan_amortissement")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LignePlanAmortissement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "immobilisation_id", nullable = false)
    @JsonBackReference
    private Immobilisation immobilisation;

    @Column(nullable = false)
    private Integer annee;

    @Column(name = "mois_amortis", nullable = false)
    private Integer moisAmortis = 12;

    @Column(name = "base_calcul", precision = 15, scale = 2, nullable = false)
    private BigDecimal baseCalcul = BigDecimal.ZERO;

    @Column(name = "taux_applique", precision = 8, scale = 4, nullable = false)
    private BigDecimal tauxApplique = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal dotation = BigDecimal.ZERO;

    @Column(name = "cumul_amortissement", precision = 15, scale = 2, nullable = false)
    private BigDecimal cumulAmortissement = BigDecimal.ZERO;

    @Column(name = "valeur_nette_fin", precision = 15, scale = 2, nullable = false)
    private BigDecimal valeurNetteFin = BigDecimal.ZERO;

    @Column(name = "mode_lineaire_bascule", nullable = false)
    private Boolean modeLineaireBascule = false;

    @Column(nullable = false)
    private Boolean comptabilisee = false;

    @Column(name = "date_comptabilisation")
    private LocalDate dateComptabilisation;

    @Column(name = "ecriture_id")
    private Long ecritureId;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public LignePlanAmortissement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Immobilisation getImmobilisation() { return immobilisation; }
    public void setImmobilisation(Immobilisation immobilisation) { this.immobilisation = immobilisation; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public Integer getMoisAmortis() { return moisAmortis; }
    public void setMoisAmortis(Integer moisAmortis) { this.moisAmortis = moisAmortis; }

    public BigDecimal getBaseCalcul() { return baseCalcul; }
    public void setBaseCalcul(BigDecimal baseCalcul) { this.baseCalcul = baseCalcul; }

    public BigDecimal getTauxApplique() { return tauxApplique; }
    public void setTauxApplique(BigDecimal tauxApplique) { this.tauxApplique = tauxApplique; }

    public BigDecimal getDotation() { return dotation; }
    public void setDotation(BigDecimal dotation) { this.dotation = dotation; }

    public BigDecimal getCumulAmortissement() { return cumulAmortissement; }
    public void setCumulAmortissement(BigDecimal cumulAmortissement) { this.cumulAmortissement = cumulAmortissement; }

    public BigDecimal getValeurNetteFin() { return valeurNetteFin; }
    public void setValeurNetteFin(BigDecimal valeurNetteFin) { this.valeurNetteFin = valeurNetteFin; }

    public Boolean getModeLineaireBascule() { return modeLineaireBascule; }
    public void setModeLineaireBascule(Boolean modeLineaireBascule) { this.modeLineaireBascule = modeLineaireBascule; }

    public Boolean getComptabilisee() { return comptabilisee; }
    public void setComptabilisee(Boolean comptabilisee) { this.comptabilisee = comptabilisee; }

    public LocalDate getDateComptabilisation() { return dateComptabilisation; }
    public void setDateComptabilisation(LocalDate dateComptabilisation) { this.dateComptabilisation = dateComptabilisation; }

    public Long getEcritureId() { return ecritureId; }
    public void setEcritureId(Long ecritureId) { this.ecritureId = ecritureId; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
