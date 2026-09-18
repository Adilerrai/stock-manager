package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StatutPasserelleDTO {

    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Ventes
    private int facturesVentesTotal = 0;
    private int facturesVentesDeversees = 0;
    private int facturesVentesEnAttente = 0;
    private BigDecimal montantVentesEnAttente = BigDecimal.ZERO;

    // Achats
    private int facturesAchatsTotal = 0;
    private int facturesAchatsDeversees = 0;
    private int facturesAchatsEnAttente = 0;
    private BigDecimal montantAchatsEnAttente = BigDecimal.ZERO;

    // Règlements Clients (Encaissements)
    private int paiementsClientsTotal = 0;
    private int paiementsClientsDeversees = 0;
    private int paiementsClientsEnAttente = 0;
    private BigDecimal montantPaiementsEnAttente = BigDecimal.ZERO;

    // Règlements Fournisseurs (Décaissements)
    private int reglementsFournisseursTotal = 0;
    private int reglementsFournisseursDeversees = 0;
    private int reglementsFournisseursEnAttente = 0;
    private BigDecimal montantReglementsEnAttente = BigDecimal.ZERO;

    // Global
    private int totalPiecesEnAttente = 0;
    private boolean toutEstSynchronise = true;

    public StatutPasserelleDTO() {}

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getFacturesVentesTotal() { return facturesVentesTotal; }
    public void setFacturesVentesTotal(int facturesVentesTotal) { this.facturesVentesTotal = facturesVentesTotal; }

    public int getFacturesVentesDeversees() { return facturesVentesDeversees; }
    public void setFacturesVentesDeversees(int facturesVentesDeversees) { this.facturesVentesDeversees = facturesVentesDeversees; }

    public int getFacturesVentesEnAttente() { return facturesVentesEnAttente; }
    public void setFacturesVentesEnAttente(int facturesVentesEnAttente) { this.facturesVentesEnAttente = facturesVentesEnAttente; }

    public BigDecimal getMontantVentesEnAttente() { return montantVentesEnAttente; }
    public void setMontantVentesEnAttente(BigDecimal montantVentesEnAttente) { this.montantVentesEnAttente = montantVentesEnAttente; }

    public int getFacturesAchatsTotal() { return facturesAchatsTotal; }
    public void setFacturesAchatsTotal(int facturesAchatsTotal) { this.facturesAchatsTotal = facturesAchatsTotal; }

    public int getFacturesAchatsDeversees() { return facturesAchatsDeversees; }
    public void setFacturesAchatsDeversees(int facturesAchatsDeversees) { this.facturesAchatsDeversees = facturesAchatsDeversees; }

    public int getFacturesAchatsEnAttente() { return facturesAchatsEnAttente; }
    public void setFacturesAchatsEnAttente(int facturesAchatsEnAttente) { this.facturesAchatsEnAttente = facturesAchatsEnAttente; }

    public BigDecimal getMontantAchatsEnAttente() { return montantAchatsEnAttente; }
    public void setMontantAchatsEnAttente(BigDecimal montantAchatsEnAttente) { this.montantAchatsEnAttente = montantAchatsEnAttente; }

    public int getPaiementsClientsTotal() { return paiementsClientsTotal; }
    public void setPaiementsClientsTotal(int paiementsClientsTotal) { this.paiementsClientsTotal = paiementsClientsTotal; }

    public int getPaiementsClientsDeversees() { return paiementsClientsDeversees; }
    public void setPaiementsClientsDeversees(int paiementsClientsDeversees) { this.paiementsClientsDeversees = paiementsClientsDeversees; }

    public int getPaiementsClientsEnAttente() { return paiementsClientsEnAttente; }
    public void setPaiementsClientsEnAttente(int paiementsClientsEnAttente) { this.paiementsClientsEnAttente = paiementsClientsEnAttente; }

    public BigDecimal getMontantPaiementsEnAttente() { return montantPaiementsEnAttente; }
    public void setMontantPaiementsEnAttente(BigDecimal montantPaiementsEnAttente) { this.montantPaiementsEnAttente = montantPaiementsEnAttente; }

    public int getReglementsFournisseursTotal() { return reglementsFournisseursTotal; }
    public void setReglementsFournisseursTotal(int reglementsFournisseursTotal) { this.reglementsFournisseursTotal = reglementsFournisseursTotal; }

    public int getReglementsFournisseursDeversees() { return reglementsFournisseursDeversees; }
    public void setReglementsFournisseursDeversees(int reglementsFournisseursDeversees) { this.reglementsFournisseursDeversees = reglementsFournisseursDeversees; }

    public int getReglementsFournisseursEnAttente() { return reglementsFournisseursEnAttente; }
    public void setReglementsFournisseursEnAttente(int reglementsFournisseursEnAttente) { this.reglementsFournisseursEnAttente = reglementsFournisseursEnAttente; }

    public BigDecimal getMontantReglementsEnAttente() { return montantReglementsEnAttente; }
    public void setMontantReglementsEnAttente(BigDecimal montantReglementsEnAttente) { this.montantReglementsEnAttente = montantReglementsEnAttente; }

    public int getTotalPiecesEnAttente() { return totalPiecesEnAttente; }
    public void setTotalPiecesEnAttente(int totalPiecesEnAttente) { this.totalPiecesEnAttente = totalPiecesEnAttente; }

    public boolean isToutEstSynchronise() { return toutEstSynchronise; }
    public void setToutEstSynchronise(boolean toutEstSynchronise) { this.toutEstSynchronise = toutEstSynchronise; }
}
