package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.persistent.enums.EvenementPaiement;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "conditions_paiement_lignes")
public class ConditionPaiementLigne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_paiement_id", nullable = false)
    private ConditionPaiement conditionPaiement;

    @Column(nullable = false)
    private Integer ordre = 1;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal pourcentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EvenementPaiement evenement;

    @Column(name = "delai_jours")
    private Integer delaiJours = 0;

    public ConditionPaiementLigne() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConditionPaiement getConditionPaiement() { return conditionPaiement; }
    public void setConditionPaiement(ConditionPaiement conditionPaiement) { this.conditionPaiement = conditionPaiement; }

    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }

    public BigDecimal getPourcentage() { return pourcentage; }
    public void setPourcentage(BigDecimal pourcentage) { this.pourcentage = pourcentage; }

    public EvenementPaiement getEvenement() { return evenement; }
    public void setEvenement(EvenementPaiement evenement) { this.evenement = evenement; }

    public Integer getDelaiJours() { return delaiJours; }
    public void setDelaiJours(Integer delaiJours) { this.delaiJours = delaiJours; }
}
