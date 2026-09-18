package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LignePlanAmortissementDTO {

    private Long id;
    private Integer annee;
    private Integer moisAmortis;
    private BigDecimal baseCalcul;
    private BigDecimal tauxApplique;
    private BigDecimal dotation;
    private BigDecimal cumulAmortissement;
    private BigDecimal valeurNetteFin;
    private Boolean modeLineaireBascule;
    private Boolean comptabilisee;
    private LocalDate dateComptabilisation;
    private Long ecritureId;

    public LignePlanAmortissementDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}
