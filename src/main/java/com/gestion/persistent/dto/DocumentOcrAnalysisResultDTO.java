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
    private Long documentId;
    private String urlVisualisation;
    private String nomBanque;
    private String numeroCheque;
    private String numeroCommandeExterne;

    // Métadonnées Devis International / Vigilance Importation
    private String devise;
    private BigDecimal tauxChangeEstime;
    private String incoterm;
    private String lieuIncoterm;
    private String portEmbarquement;
    private String validiteOffre;
    private String codeDouanierSh;
    private BigDecimal acompteExigePct;
    private String conditionsPaiement;

    public DocumentOcrAnalysisResultDTO() {}

    public String getNumeroCommandeExterne() {
        return numeroCommandeExterne != null ? numeroCommandeExterne : numeroPiece;
    }

    public void setNumeroCommandeExterne(String numeroCommandeExterne) {
        this.numeroCommandeExterne = numeroCommandeExterne;
    }

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

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getUrlVisualisation() {
        return urlVisualisation;
    }

    public void setUrlVisualisation(String urlVisualisation) {
        this.urlVisualisation = urlVisualisation;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getNumeroCheque() {
        return numeroCheque;
    }

    public void setNumeroCheque(String numeroCheque) {
        this.numeroCheque = numeroCheque;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public BigDecimal getTauxChangeEstime() {
        return tauxChangeEstime;
    }

    public void setTauxChangeEstime(BigDecimal tauxChangeEstime) {
        this.tauxChangeEstime = tauxChangeEstime;
    }

    public String getIncoterm() {
        return incoterm;
    }

    public void setIncoterm(String incoterm) {
        this.incoterm = incoterm;
    }

    public String getLieuIncoterm() {
        return lieuIncoterm;
    }

    public void setLieuIncoterm(String lieuIncoterm) {
        this.lieuIncoterm = lieuIncoterm;
    }

    public String getPortEmbarquement() {
        return portEmbarquement;
    }

    public void setPortEmbarquement(String portEmbarquement) {
        this.portEmbarquement = portEmbarquement;
    }

    public String getValiditeOffre() {
        return validiteOffre;
    }

    public void setValiditeOffre(String validiteOffre) {
        this.validiteOffre = validiteOffre;
    }

    public String getCodeDouanierSh() {
        return codeDouanierSh;
    }

    public void setCodeDouanierSh(String codeDouanierSh) {
        this.codeDouanierSh = codeDouanierSh;
    }

    public BigDecimal getAcompteExigePct() {
        return acompteExigePct;
    }

    public void setAcompteExigePct(BigDecimal acompteExigePct) {
        this.acompteExigePct = acompteExigePct;
    }

    public String getConditionsPaiement() {
        return conditionsPaiement;
    }

    public void setConditionsPaiement(String conditionsPaiement) {
        this.conditionsPaiement = conditionsPaiement;
    }
}
