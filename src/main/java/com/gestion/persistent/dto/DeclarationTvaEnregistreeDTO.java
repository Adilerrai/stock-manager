package com.gestion.persistent.dto;

import com.gestion.persistent.enums.RegimeTva;
import com.gestion.persistent.enums.StatutDeclarationTva;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeclarationTvaEnregistreeDTO {
    private Long id;
    private String periode;
    private RegimeTva regime;
    private BigDecimal tvaCollectee;
    private BigDecimal tvaDeductibleCharges;
    private BigDecimal tvaDeductibleImmo;
    private BigDecimal tvaDeductibleTotal;
    private BigDecimal creditTvaAnterieur;
    private BigDecimal tvaAPayer;
    private BigDecimal creditTvaReportable;
    private BigDecimal prorata;
    private BigDecimal totalVentesHT;
    private BigDecimal totalAchatsHT;
    private StatutDeclarationTva statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateValidation;
    private String valideePar;
    private String notes;

    public DeclarationTvaEnregistreeDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public RegimeTva getRegime() { return regime; }
    public void setRegime(RegimeTva regime) { this.regime = regime; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee; }

    public BigDecimal getTvaDeductibleCharges() { return tvaDeductibleCharges; }
    public void setTvaDeductibleCharges(BigDecimal tvaDeductibleCharges) { this.tvaDeductibleCharges = tvaDeductibleCharges; }

    public BigDecimal getTvaDeductibleImmo() { return tvaDeductibleImmo; }
    public void setTvaDeductibleImmo(BigDecimal tvaDeductibleImmo) { this.tvaDeductibleImmo = tvaDeductibleImmo; }

    public BigDecimal getTvaDeductibleTotal() { return tvaDeductibleTotal; }
    public void setTvaDeductibleTotal(BigDecimal tvaDeductibleTotal) { this.tvaDeductibleTotal = tvaDeductibleTotal; }

    public BigDecimal getCreditTvaAnterieur() { return creditTvaAnterieur; }
    public void setCreditTvaAnterieur(BigDecimal creditTvaAnterieur) { this.creditTvaAnterieur = creditTvaAnterieur; }

    public BigDecimal getTvaAPayer() { return tvaAPayer; }
    public void setTvaAPayer(BigDecimal tvaAPayer) { this.tvaAPayer = tvaAPayer; }

    public BigDecimal getCreditTvaReportable() { return creditTvaReportable; }
    public void setCreditTvaReportable(BigDecimal creditTvaReportable) { this.creditTvaReportable = creditTvaReportable; }

    public BigDecimal getProrata() { return prorata; }
    public void setProrata(BigDecimal prorata) { this.prorata = prorata; }

    public BigDecimal getTotalVentesHT() { return totalVentesHT; }
    public void setTotalVentesHT(BigDecimal totalVentesHT) { this.totalVentesHT = totalVentesHT; }

    public BigDecimal getTotalAchatsHT() { return totalAchatsHT; }
    public void setTotalAchatsHT(BigDecimal totalAchatsHT) { this.totalAchatsHT = totalAchatsHT; }

    public StatutDeclarationTva getStatut() { return statut; }
    public void setStatut(StatutDeclarationTva statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }

    public String getValideePar() { return valideePar; }
    public void setValideePar(String valideePar) { this.valideePar = valideePar; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
