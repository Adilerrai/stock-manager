package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AppliquerModeleRequest {

    private String codeModele; // ex: "LOYER_COMMERCIAL", "HONORAIRES_RAS"
    private BigDecimal montantBase = BigDecimal.ZERO; // Montant TTC ou HT selon le modèle
    private boolean baseEstTtc = true; // Par défaut, l'utilisateur saisit le montant TTC de sa facture
    private LocalDate dateEcriture;
    private String referencePiece;
    private String libelleComplement; // ex: "Loyer Septembre 2026 - Agence Hassan"
    private String journalCode; // Facultatif, surcharge le journal par défaut du modèle
    private String tiersNumeroCompte; // Surcharge du compte tiers si compte auxiliaire personnalisé (ex: "44110012")

    public AppliquerModeleRequest() {}

    public String getCodeModele() { return codeModele; }
    public void setCodeModele(String codeModele) { this.codeModele = codeModele; }

    public BigDecimal getMontantBase() { return montantBase; }
    public void setMontantBase(BigDecimal montantBase) { this.montantBase = montantBase; }

    public boolean isBaseEstTtc() { return baseEstTtc; }
    public void setBaseEstTtc(boolean baseEstTtc) { this.baseEstTtc = baseEstTtc; }

    public LocalDate getDateEcriture() { return dateEcriture; }
    public void setDateEcriture(LocalDate dateEcriture) { this.dateEcriture = dateEcriture; }

    public String getReferencePiece() { return referencePiece; }
    public void setReferencePiece(String referencePiece) { this.referencePiece = referencePiece; }

    public String getLibelleComplement() { return libelleComplement; }
    public void setLibelleComplement(String libelleComplement) { this.libelleComplement = libelleComplement; }

    public String getJournalCode() { return journalCode; }
    public void setJournalCode(String journalCode) { this.journalCode = journalCode; }

    public String getTiersNumeroCompte() { return tiersNumeroCompte; }
    public void setTiersNumeroCompte(String tiersNumeroCompte) { this.tiersNumeroCompte = tiersNumeroCompte; }
}
