package com.gestion.persistent.dto;

import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.StatutEffet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReglementClientRequest {

    private Long clientId;
    private BigDecimal montant;
    private ModePaiement modePaiement;
    private LocalDateTime datePaiement;
    private String notes;

    // Métadonnées Chèque / Effet
    private String numeroCheque;
    private String nomBanque;
    private LocalDate dateEcheance;
    private StatutEffet statutCheque;

    // Stratégie d'imputation : "GLOBAL_FIFO", "MANUELLE", "ACOMPTE"
    private String typeAffectation;

    // Liste des imputations si MANUELLE
    private List<AffectationItemDTO> affectations = new ArrayList<>();

    public ReglementClientRequest() {}

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public ModePaiement getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(ModePaiement modePaiement) {
        this.modePaiement = modePaiement;
    }

    public LocalDateTime getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDateTime datePaiement) {
        this.datePaiement = datePaiement;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public StatutEffet getStatutCheque() {
        return statutCheque;
    }

    public void setStatutCheque(StatutEffet statutCheque) {
        this.statutCheque = statutCheque;
    }

    public String getTypeAffectation() {
        return typeAffectation;
    }

    public void setTypeAffectation(String typeAffectation) {
        this.typeAffectation = typeAffectation;
    }

    public List<AffectationItemDTO> getAffectations() {
        return affectations;
    }

    public void setAffectations(List<AffectationItemDTO> affectations) {
        this.affectations = affectations;
    }
}
