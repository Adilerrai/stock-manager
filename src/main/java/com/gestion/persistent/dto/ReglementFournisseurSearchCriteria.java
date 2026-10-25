package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gestion.persistent.enums.ModePaiement;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReglementFournisseurSearchCriteria {

    private String numeroReglement;
    private ModePaiement modePaiement;
    private Long factureAchatId;
    private String numeroFacture;
    private Long fournisseurId;
    private String referencePaiement;
    private String numeroCheque;
    private String nomBanque;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDateTime dateDebut;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDateTime dateFin;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDateTime dateEcheanceDebut;

    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    private LocalDateTime dateEcheanceFin;

    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long societeId;
    private String query;

    public ReglementFournisseurSearchCriteria() {}

    public String getNumeroReglement() {
        return numeroReglement;
    }

    public void setNumeroReglement(String numeroReglement) {
        this.numeroReglement = numeroReglement;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public void setModeReglement(ModePaiement modeReglement) {
        if (this.modePaiement == null) {
            this.modePaiement = modeReglement;
        }
    }

    public Long getFactureAchatId() {
        return factureAchatId;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }

    public void setFactureId(Long factureId) {
        if (this.factureAchatId == null) {
            this.factureAchatId = factureId;
        }
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public void setNumeroFactureAchat(String numeroFactureAchat) {
        if (this.numeroFacture == null) {
            this.numeroFacture = numeroFactureAchat;
        }
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public String getReferencePaiement() {
        return referencePaiement;
    }

    public void setReferencePaiement(String referencePaiement) {
        this.referencePaiement = referencePaiement;
    }

    public String getNumeroCheque() {
        return numeroCheque;
    }

    public void setNumeroCheque(String numeroCheque) {
        this.numeroCheque = numeroCheque;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public LocalDateTime getDateEcheanceDebut() {
        return dateEcheanceDebut;
    }

    public void setDateEcheanceDebut(LocalDateTime dateEcheanceDebut) {
        this.dateEcheanceDebut = dateEcheanceDebut;
    }

    public LocalDateTime getDateEcheanceFin() {
        return dateEcheanceFin;
    }

    public void setDateEcheanceFin(LocalDateTime dateEcheanceFin) {
        this.dateEcheanceFin = dateEcheanceFin;
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

    public Long getSocieteId() {
        return societeId;
    }

    public void setSocieteId(Long societeId) {
        this.societeId = societeId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
