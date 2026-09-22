package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Relevé de déduction consolidé (Article 112 du CGI Maroc)
 * avec contrôle interne par rapprochement bancaire.
 */
public class ReleveDeductionTvaDTO {

    // En-tête officiel
    private String raisonSociale;
    private String identifiantFiscal;
    private String ice;
    private Integer annee;
    private String periode;
    private String regime = "Encaissement";
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean seulementRapproches = false;

    // Totaux consolidés
    private BigDecimal totalMontantHt = BigDecimal.ZERO;
    private BigDecimal totalMontantTva = BigDecimal.ZERO;
    private BigDecimal totalMontantTtc = BigDecimal.ZERO;
    private BigDecimal totalMontantDeductible = BigDecimal.ZERO;
    private BigDecimal totalTvaRapprochee = BigDecimal.ZERO;
    private BigDecimal totalTvaNonRapprochee = BigDecimal.ZERO;

    // Compteurs de contrôle
    private int nbLignesTotal = 0;
    private int nbLignesRapprochees = 0;
    private int nbLignesNonRapprochees = 0;
    private BigDecimal tauxCouvertureRapprochement = BigDecimal.ZERO;

    // Détail des lignes
    private List<LigneReleveDeductionDTO> lignes = new ArrayList<>();

    public ReleveDeductionTvaDTO() {}

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public String getRegime() { return regime; }
    public void setRegime(String regime) { this.regime = regime; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Boolean getSeulementRapproches() { return seulementRapproches; }
    public void setSeulementRapproches(Boolean seulementRapproches) { this.seulementRapproches = seulementRapproches; }

    public BigDecimal getTotalMontantHt() { return totalMontantHt; }
    public void setTotalMontantHt(BigDecimal totalMontantHt) { this.totalMontantHt = totalMontantHt != null ? totalMontantHt : BigDecimal.ZERO; }

    public BigDecimal getTotalMontantTva() { return totalMontantTva; }
    public void setTotalMontantTva(BigDecimal totalMontantTva) { this.totalMontantTva = totalMontantTva != null ? totalMontantTva : BigDecimal.ZERO; }

    public BigDecimal getTotalMontantTtc() { return totalMontantTtc; }
    public void setTotalMontantTtc(BigDecimal totalMontantTtc) { this.totalMontantTtc = totalMontantTtc != null ? totalMontantTtc : BigDecimal.ZERO; }

    public BigDecimal getTotalMontantDeductible() { return totalMontantDeductible; }
    public void setTotalMontantDeductible(BigDecimal totalMontantDeductible) { this.totalMontantDeductible = totalMontantDeductible != null ? totalMontantDeductible : BigDecimal.ZERO; }

    public BigDecimal getTotalTvaRapprochee() { return totalTvaRapprochee; }
    public void setTotalTvaRapprochee(BigDecimal totalTvaRapprochee) { this.totalTvaRapprochee = totalTvaRapprochee != null ? totalTvaRapprochee : BigDecimal.ZERO; }

    public BigDecimal getTotalTvaNonRapprochee() { return totalTvaNonRapprochee; }
    public void setTotalTvaNonRapprochee(BigDecimal totalTvaNonRapprochee) { this.totalTvaNonRapprochee = totalTvaNonRapprochee != null ? totalTvaNonRapprochee : BigDecimal.ZERO; }

    public int getNbLignesTotal() { return nbLignesTotal; }
    public void setNbLignesTotal(int nbLignesTotal) { this.nbLignesTotal = nbLignesTotal; }

    public int getNbLignesRapprochees() { return nbLignesRapprochees; }
    public void setNbLignesRapprochees(int nbLignesRapprochees) { this.nbLignesRapprochees = nbLignesRapprochees; }

    public int getNbLignesNonRapprochees() { return nbLignesNonRapprochees; }
    public void setNbLignesNonRapprochees(int nbLignesNonRapprochees) { this.nbLignesNonRapprochees = nbLignesNonRapprochees; }

    public BigDecimal getTauxCouvertureRapprochement() { return tauxCouvertureRapprochement; }
    public void setTauxCouvertureRapprochement(BigDecimal tauxCouvertureRapprochement) { this.tauxCouvertureRapprochement = tauxCouvertureRapprochement != null ? tauxCouvertureRapprochement : BigDecimal.ZERO; }

    public List<LigneReleveDeductionDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneReleveDeductionDTO> lignes) { this.lignes = lignes != null ? lignes : new ArrayList<>(); }
}
