package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DocumentOcrAnalysisResultDTO {

    private String typeDocument; // FACTURE_ACHAT, COMMANDE_CLIENT, BON_LIVRAISON_FOURNISSEUR
    private String numeroPiece;
    private String datePiece;
    private String dateEcheance;

    private Long tierId;
    private String tierNom;
    private String tierIce;
    private String tierTelephone;
    private String tierAdresse;
    private boolean tierMatched;
    private String tierType; // FOURNISSEUR, CLIENT

    private BigDecimal montantHT = BigDecimal.ZERO;
    private BigDecimal montantTVA = BigDecimal.ZERO;
    private BigDecimal montantTTC = BigDecimal.ZERO;

    private List<LigneDocumentOcrDTO> lignes = new ArrayList<>();
    private String rawOcrText;
    private Double confidenceScore = 0.9;
    private String statut; // PRET_A_ENREGISTRER, ATTENTION, ENREGISTRE
    private String message;
    private Long savedEntityId;

    public DocumentOcrAnalysisResultDTO() {}

    public String getTypeDocument() {
        return typeDocument;
    }

    public void setTypeDocument(String typeDocument) {
        this.typeDocument = typeDocument;
    }

    public String getNumeroPiece() {
        return numeroPiece;
    }

    public void setNumeroPiece(String numeroPiece) {
        this.numeroPiece = numeroPiece;
    }

    public String getDatePiece() {
        return datePiece;
    }

    public void setDatePiece(String datePiece) {
        this.datePiece = datePiece;
    }

    public String getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(String dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public Long getTierId() {
        return tierId;
    }

    public void setTierId(Long tierId) {
        this.tierId = tierId;
    }

    public String getTierNom() {
        return tierNom;
    }

    public void setTierNom(String tierNom) {
        this.tierNom = tierNom;
    }

    public String getTierIce() {
        return tierIce;
    }

    public void setTierIce(String tierIce) {
        this.tierIce = tierIce;
    }

    public String getTierTelephone() {
        return tierTelephone;
    }

    public void setTierTelephone(String tierTelephone) {
        this.tierTelephone = tierTelephone;
    }

    public String getTierAdresse() {
        return tierAdresse;
    }

    public void setTierAdresse(String tierAdresse) {
        this.tierAdresse = tierAdresse;
    }

    public boolean isTierMatched() {
        return tierMatched;
    }

    public void setTierMatched(boolean tierMatched) {
        this.tierMatched = tierMatched;
    }

    public String getTierType() {
        return tierType;
    }

    public void setTierType(String tierType) {
        this.tierType = tierType;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public List<LigneDocumentOcrDTO> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneDocumentOcrDTO> lignes) {
        this.lignes = lignes;
    }

    public String getRawOcrText() {
        return rawOcrText;
    }

    public void setRawOcrText(String rawOcrText) {
        this.rawOcrText = rawOcrText;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getSavedEntityId() {
        return savedEntityId;
    }

    public void setSavedEntityId(Long savedEntityId) {
        this.savedEntityId = savedEntityId;
    }
}
