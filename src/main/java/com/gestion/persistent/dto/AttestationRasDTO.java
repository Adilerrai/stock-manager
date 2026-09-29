package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeOperationRas;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modèle officiel de l'Attestation de Retenue à la Source (CGI Maroc - Art. 117-VI).
 * Document légal remis au fournisseur attestant de la retenue opérée et reversée au Trésor.
 */
public class AttestationRasDTO {

    private Long id;
    private String numeroAttestation; // Ex: "AT-RAS-2026-0001"
    private LocalDate dateAttestation = LocalDate.now();
    private Integer exerciceAnnee;
    private Integer periodeMois;
    private Integer periodeTrimestre;

    // Identité de la Société Acheteuse (Déclarante)
    private Long societeId;
    private String societeRaisonSociale;
    private String societeIce;
    private String societeIf;
    private String societeRc;
    private String societeAdresse;
    private String societeVille;

    // Identité du Fournisseur (Bénéficiaire)
    private Long fournisseurId;
    private String fournisseurNom;
    private String fournisseurIce;
    private String fournisseurIf;
    private String fournisseurRc;
    private String fournisseurAdresse;
    private String fournisseurVille;

    // Données de la Facture
    private Long factureAchatId;
    private String factureNumero;
    private LocalDate factureDate;
    private BigDecimal montantHt = BigDecimal.ZERO;
    private BigDecimal tauxTva = new BigDecimal("20.0");
    private BigDecimal montantTva = BigDecimal.ZERO;
    private BigDecimal montantTtc = BigDecimal.ZERO;

    // Données du Règlement et de la Retenue
    private Long reglementId;
    private LocalDate datePaiement;
    private String modePaiement;
    private String referencePaiement;
    private TypeOperationRas typeOperation;
    private Boolean attestationFiscaleFournisseurPresente;
    private BigDecimal tauxRas = BigDecimal.ZERO; // ex: 75.0 ou 100.0
    private BigDecimal montantRas = BigDecimal.ZERO;
    private BigDecimal montantNetPaye = BigDecimal.ZERO;

    // Mentions Légales
    private String mentionLegale = "Attestation délivrée en application de l'article 117-VI du Code Général des Impôts (CGI Maroc).";
    private String statut = "VALIDEE"; // VALIDEE, TRANSMISE_DGI, ARCHIVEE

    public AttestationRasDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroAttestation() {
        return numeroAttestation;
    }

    public void setNumeroAttestation(String numeroAttestation) {
        this.numeroAttestation = numeroAttestation;
    }

    public LocalDate getDateAttestation() {
        return dateAttestation;
    }

    public void setDateAttestation(LocalDate dateAttestation) {
        this.dateAttestation = dateAttestation;
    }

    public Integer getExerciceAnnee() {
        return exerciceAnnee;
    }

    public void setExerciceAnnee(Integer exerciceAnnee) {
        this.exerciceAnnee = exerciceAnnee;
    }

    public Integer getPeriodeMois() {
        return periodeMois;
    }

    public void setPeriodeMois(Integer periodeMois) {
        this.periodeMois = periodeMois;
    }

    public Integer getPeriodeTrimestre() {
        return periodeTrimestre;
    }

    public void setPeriodeTrimestre(Integer periodeTrimestre) {
        this.periodeTrimestre = periodeTrimestre;
    }

    public Long getSocieteId() {
        return societeId;
    }

    public void setSocieteId(Long societeId) {
        this.societeId = societeId;
    }

    public String getSocieteRaisonSociale() {
        return societeRaisonSociale;
    }

    public void setSocieteRaisonSociale(String societeRaisonSociale) {
        this.societeRaisonSociale = societeRaisonSociale;
    }

    public String getSocieteIce() {
        return societeIce;
    }

    public void setSocieteIce(String societeIce) {
        this.societeIce = societeIce;
    }

    public String getSocieteIf() {
        return societeIf;
    }

    public void setSocieteIf(String societeIf) {
        this.societeIf = societeIf;
    }

    public String getSocieteRc() {
        return societeRc;
    }

    public void setSocieteRc(String societeRc) {
        this.societeRc = societeRc;
    }

    public String getSocieteAdresse() {
        return societeAdresse;
    }

    public void setSocieteAdresse(String societeAdresse) {
        this.societeAdresse = societeAdresse;
    }

    public String getSocieteVille() {
        return societeVille;
    }

    public void setSocieteVille(String societeVille) {
        this.societeVille = societeVille;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public String getFournisseurNom() {
        return fournisseurNom;
    }

    public void setFournisseurNom(String fournisseurNom) {
        this.fournisseurNom = fournisseurNom;
    }

    public String getFournisseurIce() {
        return fournisseurIce;
    }

    public void setFournisseurIce(String fournisseurIce) {
        this.fournisseurIce = fournisseurIce;
    }

    public String getFournisseurIf() {
        return fournisseurIf;
    }

    public void setFournisseurIf(String fournisseurIf) {
        this.fournisseurIf = fournisseurIf;
    }

    public String getFournisseurRc() {
        return fournisseurRc;
    }

    public void setFournisseurRc(String fournisseurRc) {
        this.fournisseurRc = fournisseurRc;
    }

    public String getFournisseurAdresse() {
        return fournisseurAdresse;
    }

    public void setFournisseurAdresse(String fournisseurAdresse) {
        this.fournisseurAdresse = fournisseurAdresse;
    }

    public String getFournisseurVille() {
        return fournisseurVille;
    }

    public void setFournisseurVille(String fournisseurVille) {
        this.fournisseurVille = fournisseurVille;
    }

    public Long getFactureAchatId() {
        return factureAchatId;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }

    public String getFactureNumero() {
        return factureNumero;
    }

    public void setFactureNumero(String factureNumero) {
        this.factureNumero = factureNumero;
    }

    public LocalDate getFactureDate() {
        return factureDate;
    }

    public void setFactureDate(LocalDate factureDate) {
        this.factureDate = factureDate;
    }

    public BigDecimal getMontantHt() {
        return montantHt;
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public void setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
    }

    public BigDecimal getMontantTtc() {
        return montantTtc;
    }

    public void setMontantTtc(BigDecimal montantTtc) {
        this.montantTtc = montantTtc;
    }

    public Long getReglementId() {
        return reglementId;
    }

    public void setReglementId(Long reglementId) {
        this.reglementId = reglementId;
    }

    public LocalDate getDatePaiement() {
        return datePaiement;
    }

    public void setDatePaiement(LocalDate datePaiement) {
        this.datePaiement = datePaiement;
    }

    public String getModePaiement() {
        return modePaiement;
    }

    public void setModePaiement(String modePaiement) {
        this.modePaiement = modePaiement;
    }

    public String getReferencePaiement() {
        return referencePaiement;
    }

    public void setReferencePaiement(String referencePaiement) {
        this.referencePaiement = referencePaiement;
    }

    public TypeOperationRas getTypeOperation() {
        return typeOperation;
    }

    public void setTypeOperation(TypeOperationRas typeOperation) {
        this.typeOperation = typeOperation;
    }

    public Boolean getAttestationFiscaleFournisseurPresente() {
        return attestationFiscaleFournisseurPresente;
    }

    public void setAttestationFiscaleFournisseurPresente(Boolean attestationFiscaleFournisseurPresente) {
        this.attestationFiscaleFournisseurPresente = attestationFiscaleFournisseurPresente;
    }

    public BigDecimal getTauxRas() {
        return tauxRas;
    }

    public void setTauxRas(BigDecimal tauxRas) {
        this.tauxRas = tauxRas;
    }

    public BigDecimal getMontantRas() {
        return montantRas;
    }

    public void setMontantRas(BigDecimal montantRas) {
        this.montantRas = montantRas;
    }

    public BigDecimal getMontantNetPaye() {
        return montantNetPaye;
    }

    public void setMontantNetPaye(BigDecimal montantNetPaye) {
        this.montantNetPaye = montantNetPaye;
    }

    public String getMentionLegale() {
        return mentionLegale;
    }

    public void setMentionLegale(String mentionLegale) {
        this.mentionLegale = mentionLegale;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
