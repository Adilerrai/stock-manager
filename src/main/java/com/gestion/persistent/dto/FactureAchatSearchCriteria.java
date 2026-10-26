package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gestion.persistent.enums.StatutFacture;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FactureAchatSearchCriteria {

    private String numeroFacture;
    private Long fournisseurId;
    private StatutFacture statut;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDate dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDate dateFin;

    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private String query;
    private Long societeId;

    public FactureAchatSearchCriteria() {}

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public StatutFacture getStatut() {
        return statut;
    }

    public void setStatut(StatutFacture statut) {
        this.statut = statut;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public BigDecimal getMontantMin() {
        return montantMin;
    }

    public void setMontantMin(BigDecimal montantMin) {
        this.montantMin = montantMin;
    }

    public BigDecimal getMontantMax() {
        return montantMax;
    }

    public void setMontantMax(BigDecimal montantMax) {
        this.montantMax = montantMax;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Long getSocieteId() {
        return societeId;
    }

    public void setSocieteId(Long societeId) {
        this.societeId = societeId;
    }
}
