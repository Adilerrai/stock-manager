package com.gestion.persistent.dto;

import java.time.LocalDate;

/**
 * Requête pour générer automatiquement la pièce comptable équilibrée
 * d'un règlement fournisseur avec Retenue à la Source (Débit 4411 / Crédit 5141 + Crédit 4458).
 */
public class ComptabiliserRasRequest {

    private Long reglementId;
    private Long factureAchatId;
    private LocalDate dateEcriture;
    private String journalCode = "BQ";
    private String compteTresorerie = "51410000"; // Banque BMCE, BCP, etc.
    private String compteRas = "44580000"; // État, TVA retenue à la source
    private String libelle;

    public ComptabiliserRasRequest() {}

    public Long getReglementId() {
        return reglementId;
    }

    public void setReglementId(Long reglementId) {
        this.reglementId = reglementId;
    }

    public Long getFactureAchatId() {
        return factureAchatId;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }

    public LocalDate getDateEcriture() {
        return dateEcriture;
    }

    public void setDateEcriture(LocalDate dateEcriture) {
        this.dateEcriture = dateEcriture;
    }

    public String getJournalCode() {
        return journalCode;
    }

    public void setJournalCode(String journalCode) {
        this.journalCode = journalCode;
    }

    public String getCompteTresorerie() {
        return compteTresorerie;
    }

    public void setCompteTresorerie(String compteTresorerie) {
        this.compteTresorerie = compteTresorerie;
    }

    public String getCompteRas() {
        return compteRas;
    }

    public void setCompteRas(String compteRas) {
        this.compteRas = compteRas;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
