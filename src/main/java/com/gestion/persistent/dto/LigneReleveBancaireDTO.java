package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutRapprochement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LigneReleveBancaireDTO {
    private Long id;
    private Long releveBancaireId;
    private LocalDate dateOperation;
    private LocalDate dateValeur;
    private String libelle;
    private BigDecimal debit;
    private BigDecimal credit;
    private String reference;
    private StatutRapprochement statut;
    private Long mouvementTresorerieId;
    private String mouvementReference;
    private Long ligneEcritureId;
    private LocalDateTime dateRapprochement;

    public LigneReleveBancaireDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReleveBancaireId() {
        return releveBancaireId;
    }

    public void setReleveBancaireId(Long releveBancaireId) {
        this.releveBancaireId = releveBancaireId;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public StatutRapprochement getStatut() {
        return statut;
    }

    public void setStatut(StatutRapprochement statut) {
        this.statut = statut;
    }

    public Long getMouvementTresorerieId() {
        return mouvementTresorerieId;
    }

    public void setMouvementTresorerieId(Long mouvementTresorerieId) {
        this.mouvementTresorerieId = mouvementTresorerieId;
    }

    public String getMouvementReference() {
        return mouvementReference;
    }

    public void setMouvementReference(String mouvementReference) {
        this.mouvementReference = mouvementReference;
    }

    public Long getLigneEcritureId() {
        return ligneEcritureId;
    }

    public void setLigneEcritureId(Long ligneEcritureId) {
        this.ligneEcritureId = ligneEcritureId;
    }

    public LocalDateTime getDateRapprochement() {
        return dateRapprochement;
    }

    public void setDateRapprochement(LocalDateTime dateRapprochement) {
        this.dateRapprochement = dateRapprochement;
    }

    private String suggestionContrepartieCode;
    private String suggestionContrepartieLibelle;
    private String ecritureNumeroPiece;
    private String ecritureLibelle;

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

    public String getEcritureNumeroPiece() {
        return ecritureNumeroPiece;
    }

    public void setEcritureNumeroPiece(String ecritureNumeroPiece) {
        this.ecritureNumeroPiece = ecritureNumeroPiece;
    }

    public String getEcritureLibelle() {
        return ecritureLibelle;
    }

    public void setEcritureLibelle(String ecritureLibelle) {
        this.ecritureLibelle = ecritureLibelle;
    }
}

