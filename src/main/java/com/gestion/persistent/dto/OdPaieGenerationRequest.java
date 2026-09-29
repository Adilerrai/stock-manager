package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OdPaieGenerationRequest {

    private int annee;
    private int mois;
    private LocalDate dateEcriture;
    private String journalCode = "OD";
    private String referencePiece;
    private String libelle;

    // Débits
    private BigDecimal masseSalarialeBrute = BigDecimal.ZERO;      // Compte 61711000 (Rémunération du personnel)
    private BigDecimal chargesSocialesPatronales = BigDecimal.ZERO; // Compte 61741000 (Charges sociales patronales CNSS/AMO)
    private BigDecimal retraitePatronaleCimr = BigDecimal.ZERO;     // Compte 61743000 (Cotisations patronales de retraite)

    // Crédits
    private BigDecimal netAPayerPersonnel = BigDecimal.ZERO;        // Compte 44320000 (Rémunérations dues au personnel)
    private BigDecimal cnssAPayer = BigDecimal.ZERO;                // Compte 44410000 (CNSS)
    private BigDecimal mutuelleCimrAPayer = BigDecimal.ZERO;        // Compte 44430000 (Caisse de retraite et mutuelle)
    private BigDecimal irPreleveSource = BigDecimal.ZERO;           // Compte 44525000 (État - IR prélevé à la source)

    public OdPaieGenerationRequest() {}

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public int getMois() { return mois; }
    public void setMois(int mois) { this.mois = mois; }

    public LocalDate getDateEcriture() { return dateEcriture; }
    public void setDateEcriture(LocalDate dateEcriture) { this.dateEcriture = dateEcriture; }

    public String getJournalCode() { return journalCode; }
    public void setJournalCode(String journalCode) { this.journalCode = journalCode; }

    public String getReferencePiece() { return referencePiece; }
    public void setReferencePiece(String referencePiece) { this.referencePiece = referencePiece; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getMasseSalarialeBrute() { return masseSalarialeBrute; }
    public void setMasseSalarialeBrute(BigDecimal masseSalarialeBrute) { this.masseSalarialeBrute = masseSalarialeBrute; }

    public BigDecimal getChargesSocialesPatronales() { return chargesSocialesPatronales; }
    public void setChargesSocialesPatronales(BigDecimal chargesSocialesPatronales) { this.chargesSocialesPatronales = chargesSocialesPatronales; }

    public BigDecimal getRetraitePatronaleCimr() { return retraitePatronaleCimr; }
    public void setRetraitePatronaleCimr(BigDecimal retraitePatronaleCimr) { this.retraitePatronaleCimr = retraitePatronaleCimr; }

    public BigDecimal getNetAPayerPersonnel() { return netAPayerPersonnel; }
    public void setNetAPayerPersonnel(BigDecimal netAPayerPersonnel) { this.netAPayerPersonnel = netAPayerPersonnel; }

    public BigDecimal getCnssAPayer() { return cnssAPayer; }
    public void setCnssAPayer(BigDecimal cnssAPayer) { this.cnssAPayer = cnssAPayer; }

    public BigDecimal getMutuelleCimrAPayer() { return mutuelleCimrAPayer; }
    public void setMutuelleCimrAPayer(BigDecimal mutuelleCimrAPayer) { this.mutuelleCimrAPayer = mutuelleCimrAPayer; }

    public BigDecimal getIrPreleveSource() { return irPreleveSource; }
    public void setIrPreleveSource(BigDecimal irPreleveSource) { this.irPreleveSource = irPreleveSource; }
}
