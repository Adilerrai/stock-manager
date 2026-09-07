package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FactureImpayeeDTO {
    private Long id;
    private String numeroFacture;
    private LocalDate dateFacture;
    private LocalDate dateEcheance;
    private BigDecimal montantFinal;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;
    private StatutFacture statut;

    public FactureImpayeeDTO() {}

    public FactureImpayeeDTO(Long id, String numeroFacture, LocalDate dateFacture, LocalDate dateEcheance,
                             BigDecimal montantFinal, BigDecimal montantPaye, BigDecimal montantRestant,
                             StatutFacture statut) {
        this.id = id;
        this.numeroFacture = numeroFacture;
        this.dateFacture = dateFacture;
        this.dateEcheance = dateEcheance;
        this.montantFinal = montantFinal;
        this.montantPaye = montantPaye;
        this.montantRestant = montantRestant;
        this.statut = statut;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public LocalDate getDateFacture() {
        return dateFacture;
    }

    public void setDateFacture(LocalDate dateFacture) {
        this.dateFacture = dateFacture;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontantFinal() {
        return montantFinal;
    }

    public void setMontantFinal(BigDecimal montantFinal) {
        this.montantFinal = montantFinal;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(BigDecimal montantPaye) {
        this.montantPaye = montantPaye;
    }

    public BigDecimal getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(BigDecimal montantRestant) {
        this.montantRestant = montantRestant;
    }

    public StatutFacture getStatut() {
        return statut;
    }

    public void setStatut(StatutFacture statut) {
        this.statut = statut;
    }
}
