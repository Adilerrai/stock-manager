package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ItemComparatifRapprochementDTO {
    // Opération Relevé Bancaire (Banque)
    private Long ligneReleveId;
    private LocalDate dateBanque;
    private LocalDate dateValeurBanque;
    private String libelleBanque;
    private String referenceBanque;
    private BigDecimal debitBanque = BigDecimal.ZERO;
    private BigDecimal creditBanque = BigDecimal.ZERO;

    // Statut TADBEER : "RAPPROCHE" (✓), "NON_COMPTABILISE" (⚠)
    private String statut; 

    // Écriture Comptable 5141 associée (si rapproché)
    private Long ligneEcritureId;
    private String ecritureNumeroPiece;
    private LocalDate dateCompta;
    private String libelleCompta;
    private BigDecimal debitCompta = BigDecimal.ZERO;
    private BigDecimal creditCompta = BigDecimal.ZERO;

    // Suggestion de contrepartie (pour génération écriture en 1 clic)
    private String suggestionContrepartieCode;
    private String suggestionContrepartieLibelle;

    public ItemComparatifRapprochementDTO() {}

    public Long getLigneReleveId() {
        return ligneReleveId;
    }

    public void setLigneReleveId(Long ligneReleveId) {
        this.ligneReleveId = ligneReleveId;
    }

    public LocalDate getDateBanque() {
        return dateBanque;
    }

    public void setDateBanque(LocalDate dateBanque) {
        this.dateBanque = dateBanque;
    }

    public LocalDate getDateValeurBanque() {
        return dateValeurBanque;
    }

    public void setDateValeurBanque(LocalDate dateValeurBanque) {
        this.dateValeurBanque = dateValeurBanque;
    }

    public String getLibelleBanque() {
        return libelleBanque;
    }

    public void setLibelleBanque(String libelleBanque) {
        this.libelleBanque = libelleBanque;
    }

    public String getReferenceBanque() {
        return referenceBanque;
    }

    public void setReferenceBanque(String referenceBanque) {
        this.referenceBanque = referenceBanque;
    }

    public BigDecimal getDebitBanque() {
        return debitBanque;
    }

    public void setDebitBanque(BigDecimal debitBanque) {
        this.debitBanque = debitBanque;
    }

    public BigDecimal getCreditBanque() {
        return creditBanque;
    }

    public void setCreditBanque(BigDecimal creditBanque) {
        this.creditBanque = creditBanque;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public Long getLigneEcritureId() {
        return ligneEcritureId;
    }

    public void setLigneEcritureId(Long ligneEcritureId) {
        this.ligneEcritureId = ligneEcritureId;
    }

    public String getEcritureNumeroPiece() {
        return ecritureNumeroPiece;
    }

    public void setEcritureNumeroPiece(String ecritureNumeroPiece) {
        this.ecritureNumeroPiece = ecritureNumeroPiece;
    }

    public LocalDate getDateCompta() {
        return dateCompta;
    }

    public void setDateCompta(LocalDate dateCompta) {
        this.dateCompta = dateCompta;
    }

    public String getLibelleCompta() {
        return libelleCompta;
    }

    public void setLibelleCompta(String libelleCompta) {
        this.libelleCompta = libelleCompta;
    }

    public BigDecimal getDebitCompta() {
        return debitCompta;
    }

    public void setDebitCompta(BigDecimal debitCompta) {
        this.debitCompta = debitCompta;
    }

    public BigDecimal getCreditCompta() {
        return creditCompta;
    }

    public void setCreditCompta(BigDecimal creditCompta) {
        this.creditCompta = creditCompta;
    }

    public String getSuggestionContrepartieCode() {
        return suggestionContrepartieCode;
    }

    public void setSuggestionContrepartieCode(String suggestionContrepartieCode) {
        this.suggestionContrepartieCode = suggestionContrepartieCode;
    }

    public String getSuggestionContrepartieLibelle() {
        return suggestionContrepartieLibelle;
    }

    public void setSuggestionContrepartieLibelle(String suggestionContrepartieLibelle) {
        this.suggestionContrepartieLibelle = suggestionContrepartieLibelle;
    }
}
