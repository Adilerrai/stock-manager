package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardDelaisPaiementDTO {

    private int scoreRespectDelaisPourcentage = 100;
    private BigDecimal amendeTotaleEstimeeEnCours = BigDecimal.ZERO;
    private BigDecimal detteFournisseurEnRetard = BigDecimal.ZERO;
    private BigDecimal detteFournisseurDansLesDelais = BigDecimal.ZERO;

    private int totalFacturesImpayees = 0;
    private int totalFacturesEnRetard = 0;
    private int totalFacturesARisque15Jours = 0;

    // Factures urgentes à régler immédiatement (déjà en retard ou < 15 jours)
    private List<DelaisPaiementItemDTO> alertesImmediates = new ArrayList<>();

    // Ventilation par tranches
    private BigDecimal montantRetard0a30J = BigDecimal.ZERO;
    private BigDecimal montantRetard31a60J = BigDecimal.ZERO;
    private BigDecimal montantRetardPlus60J = BigDecimal.ZERO;

    public DashboardDelaisPaiementDTO() {}

    public int getScoreRespectDelaisPourcentage() { return scoreRespectDelaisPourcentage; }
    public void setScoreRespectDelaisPourcentage(int scoreRespectDelaisPourcentage) { this.scoreRespectDelaisPourcentage = scoreRespectDelaisPourcentage; }

    public BigDecimal getAmendeTotaleEstimeeEnCours() { return amendeTotaleEstimeeEnCours; }
    public void setAmendeTotaleEstimeeEnCours(BigDecimal amendeTotaleEstimeeEnCours) { this.amendeTotaleEstimeeEnCours = amendeTotaleEstimeeEnCours; }

    public BigDecimal getDetteFournisseurEnRetard() { return detteFournisseurEnRetard; }
    public void setDetteFournisseurEnRetard(BigDecimal detteFournisseurEnRetard) { this.detteFournisseurEnRetard = detteFournisseurEnRetard; }

    public BigDecimal getDetteFournisseurDansLesDelais() { return detteFournisseurDansLesDelais; }
    public void setDetteFournisseurDansLesDelais(BigDecimal detteFournisseurDansLesDelais) { this.detteFournisseurDansLesDelais = detteFournisseurDansLesDelais; }

    public int getTotalFacturesImpayees() { return totalFacturesImpayees; }
    public void setTotalFacturesImpayees(int totalFacturesImpayees) { this.totalFacturesImpayees = totalFacturesImpayees; }

    public int getTotalFacturesEnRetard() { return totalFacturesEnRetard; }
    public void setTotalFacturesEnRetard(int totalFacturesEnRetard) { this.totalFacturesEnRetard = totalFacturesEnRetard; }

    public int getTotalFacturesARisque15Jours() { return totalFacturesARisque15Jours; }
    public void setTotalFacturesARisque15Jours(int totalFacturesARisque15Jours) { this.totalFacturesARisque15Jours = totalFacturesARisque15Jours; }

    public List<DelaisPaiementItemDTO> getAlertesImmediates() { return alertesImmediates; }
    public void setAlertesImmediates(List<DelaisPaiementItemDTO> alertesImmediates) { this.alertesImmediates = alertesImmediates; }

    public BigDecimal getMontantRetard0a30J() { return montantRetard0a30J; }
    public void setMontantRetard0a30J(BigDecimal montantRetard0a30J) { this.montantRetard0a30J = montantRetard0a30J; }

    public BigDecimal getMontantRetard31a60J() { return montantRetard31a60J; }
    public void setMontantRetard31a60J(BigDecimal montantRetard31a60J) { this.montantRetard31a60J = montantRetard31a60J; }

    public BigDecimal getMontantRetardPlus60J() { return montantRetardPlus60J; }
    public void setMontantRetardPlus60J(BigDecimal montantRetardPlus60J) { this.montantRetardPlus60J = montantRetardPlus60J; }
}
