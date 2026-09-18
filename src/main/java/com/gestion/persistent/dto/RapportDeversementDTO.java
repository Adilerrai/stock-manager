package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RapportDeversementDTO {

    private int nombreVentesDeversees = 0;
    private int nombreAchatsDeversees = 0;
    private int nombrePaiementsClientsDeversees = 0;
    private int nombreReglementsFournisseursDeversees = 0;
    private int totalPiecesDeversees = 0;

    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;

    private List<String> piecesGenerees = new ArrayList<>();
    private List<String> erreurs = new ArrayList<>();

    public RapportDeversementDTO() {}

    public int getNombreVentesDeversees() { return nombreVentesDeversees; }
    public void setNombreVentesDeversees(int nombreVentesDeversees) { this.nombreVentesDeversees = nombreVentesDeversees; }

    public int getNombreAchatsDeversees() { return nombreAchatsDeversees; }
    public void setNombreAchatsDeversees(int nombreAchatsDeversees) { this.nombreAchatsDeversees = nombreAchatsDeversees; }

    public int getNombrePaiementsClientsDeversees() { return nombrePaiementsClientsDeversees; }
    public void setNombrePaiementsClientsDeversees(int nombrePaiementsClientsDeversees) { this.nombrePaiementsClientsDeversees = nombrePaiementsClientsDeversees; }

    public int getNombreReglementsFournisseursDeversees() { return nombreReglementsFournisseursDeversees; }
    public void setNombreReglementsFournisseursDeversees(int nombreReglementsFournisseursDeversees) { this.nombreReglementsFournisseursDeversees = nombreReglementsFournisseursDeversees; }

    public int getTotalPiecesDeversees() { return totalPiecesDeversees; }
    public void setTotalPiecesDeversees(int totalPiecesDeversees) { this.totalPiecesDeversees = totalPiecesDeversees; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public List<String> getPiecesGenerees() { return piecesGenerees; }
    public void setPiecesGenerees(List<String> piecesGenerees) { this.piecesGenerees = piecesGenerees; }

    public List<String> getErreurs() { return erreurs; }
    public void setErreurs(List<String> erreurs) { this.erreurs = erreurs; }
}
