package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class CrmDashboardKPIsDTO {
    private long totalEntreprises;
    private long totalContacts;
    private long totalOpportunites;
    private BigDecimal montantPipelineTotal = BigDecimal.ZERO;
    private BigDecimal montantGagneMois = BigDecimal.ZERO;
    private long opportunitesGagnees;
    private long opportunitesPerdues;
    private long visitesPlanifieesSemaine;
    private long visitesRealiseesMois;
    private long rapportsEnAttenteValidation;
    private long rapportsEnRetard;
    private long tachesAFaire;
    private long tachesEnRetard;

    public CrmDashboardKPIsDTO() {}

    public long getTotalEntreprises() { return totalEntreprises; }
    public void setTotalEntreprises(long totalEntreprises) { this.totalEntreprises = totalEntreprises; }

    public long getTotalContacts() { return totalContacts; }
    public void setTotalContacts(long totalContacts) { this.totalContacts = totalContacts; }

    public long getTotalOpportunites() { return totalOpportunites; }
    public void setTotalOpportunites(long totalOpportunites) { this.totalOpportunites = totalOpportunites; }

    public BigDecimal getMontantPipelineTotal() { return montantPipelineTotal; }
    public void setMontantPipelineTotal(BigDecimal montantPipelineTotal) { this.montantPipelineTotal = montantPipelineTotal; }

    public BigDecimal getMontantGagneMois() { return montantGagneMois; }
    public void setMontantGagneMois(BigDecimal montantGagneMois) { this.montantGagneMois = montantGagneMois; }

    public long getOpportunitesGagnees() { return opportunitesGagnees; }
    public void setOpportunitesGagnees(long opportunitesGagnees) { this.opportunitesGagnees = opportunitesGagnees; }

    public long getOpportunitesPerdues() { return opportunitesPerdues; }
    public void setOpportunitesPerdues(long opportunitesPerdues) { this.opportunitesPerdues = opportunitesPerdues; }

    public long getVisitesPlanifieesSemaine() { return visitesPlanifieesSemaine; }
    public void setVisitesPlanifieesSemaine(long visitesPlanifieesSemaine) { this.visitesPlanifieesSemaine = visitesPlanifieesSemaine; }

    public long getVisitesRealiseesMois() { return visitesRealiseesMois; }
    public void setVisitesRealiseesMois(long visitesRealiseesMois) { this.visitesRealiseesMois = visitesRealiseesMois; }

    public long getRapportsEnAttenteValidation() { return rapportsEnAttenteValidation; }
    public void setRapportsEnAttenteValidation(long rapportsEnAttenteValidation) { this.rapportsEnAttenteValidation = rapportsEnAttenteValidation; }

    public long getRapportsEnRetard() { return rapportsEnRetard; }
    public void setRapportsEnRetard(long rapportsEnRetard) { this.rapportsEnRetard = rapportsEnRetard; }

    public long getTachesAFaire() { return tachesAFaire; }
    public void setTachesAFaire(long tachesAFaire) { this.tachesAFaire = tachesAFaire; }

    public long getTachesEnRetard() { return tachesEnRetard; }
    public void setTachesEnRetard(long tachesEnRetard) { this.tachesEnRetard = tachesEnRetard; }
}
