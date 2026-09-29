package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditFecReportDTO {

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int totalEcritures;
    private int totalLignes;

    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal ecartEquilibre = BigDecimal.ZERO;
    private boolean estParfaitementEquilibre = true;

    private int nbEcrituresBrouillons = 0;
    private int nbRupturesSequence = 0;
    private int nbComptesSansLibelle = 0;
    private int nbAuxiliairesManquants = 0;

    private int scoreConformitePourcentage = 100;
    private String statutAudit; // "CERTIFIE_CONFORME", "AVERTISSEMENTS", "NON_CONFORME_BLOQUANT"

    private List<String> anomaliesBloquantes = new ArrayList<>();
    private List<String> avertissements = new ArrayList<>();
    private List<String> pointsDeControleValides = new ArrayList<>();

    public AuditFecReportDTO() {}

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public int getTotalEcritures() { return totalEcritures; }
    public void setTotalEcritures(int totalEcritures) { this.totalEcritures = totalEcritures; }

    public int getTotalLignes() { return totalLignes; }
    public void setTotalLignes(int totalLignes) { this.totalLignes = totalLignes; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public BigDecimal getEcartEquilibre() { return ecartEquilibre; }
    public void setEcartEquilibre(BigDecimal ecartEquilibre) { this.ecartEquilibre = ecartEquilibre; }

    public boolean isEstParfaitementEquilibre() { return estParfaitementEquilibre; }
    public void setEstParfaitementEquilibre(boolean estParfaitementEquilibre) { this.estParfaitementEquilibre = estParfaitementEquilibre; }

    public int getNbEcrituresBrouillons() { return nbEcrituresBrouillons; }
    public void setNbEcrituresBrouillons(int nbEcrituresBrouillons) { this.nbEcrituresBrouillons = nbEcrituresBrouillons; }

    public int getNbRupturesSequence() { return nbRupturesSequence; }
    public void setNbRupturesSequence(int nbRupturesSequence) { this.nbRupturesSequence = nbRupturesSequence; }

    public int getNbComptesSansLibelle() { return nbComptesSansLibelle; }
    public void setNbComptesSansLibelle(int nbComptesSansLibelle) { this.nbComptesSansLibelle = nbComptesSansLibelle; }

    public int getNbAuxiliairesManquants() { return nbAuxiliairesManquants; }
    public void setNbAuxiliairesManquants(int nbAuxiliairesManquants) { this.nbAuxiliairesManquants = nbAuxiliairesManquants; }

    public int getScoreConformitePourcentage() { return scoreConformitePourcentage; }
    public void setScoreConformitePourcentage(int scoreConformitePourcentage) { this.scoreConformitePourcentage = scoreConformitePourcentage; }

    public String getStatutAudit() { return statutAudit; }
    public void setStatutAudit(String statutAudit) { this.statutAudit = statutAudit; }

    public List<String> getAnomaliesBloquantes() { return anomaliesBloquantes; }
    public void setAnomaliesBloquantes(List<String> anomaliesBloquantes) { this.anomaliesBloquantes = anomaliesBloquantes; }

    public List<String> getAvertissements() { return avertissements; }
    public void setAvertissements(List<String> avertissements) { this.avertissements = avertissements; }

    public List<String> getPointsDeControleValides() { return pointsDeControleValides; }
    public void setPointsDeControleValides(List<String> pointsDeControleValides) { this.pointsDeControleValides = pointsDeControleValides; }
}
