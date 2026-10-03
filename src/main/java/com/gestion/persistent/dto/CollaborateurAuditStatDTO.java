package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class CollaborateurAuditStatDTO {
    private String utilisateur;
    private long totalActions;
    private long creations;
    private long modifications;
    private long suppressions;
    private long validations;
    private long annulations;
    private LocalDateTime derniereDateAction;
    private String derniereEntite;

    public CollaborateurAuditStatDTO() {}

    public CollaborateurAuditStatDTO(String utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getUtilisateur() { return utilisateur; }
    public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }

    public long getTotalActions() { return totalActions; }
    public void setTotalActions(long totalActions) { this.totalActions = totalActions; }

    public long getCreations() { return creations; }
    public void setCreations(long creations) { this.creations = creations; }

    public long getModifications() { return modifications; }
    public void setModifications(long modifications) { this.modifications = modifications; }

    public long getSuppressions() { return suppressions; }
    public void setSuppressions(long suppressions) { this.suppressions = suppressions; }

    public long getValidations() { return validations; }
    public void setValidations(long validations) { this.validations = validations; }

    public long getAnnulations() { return annulations; }
    public void setAnnulations(long annulations) { this.annulations = annulations; }

    public LocalDateTime getDerniereDateAction() { return derniereDateAction; }
    public void setDerniereDateAction(LocalDateTime derniereDateAction) { this.derniereDateAction = derniereDateAction; }

    public String getDerniereEntite() { return derniereEntite; }
    public void setDerniereEntite(String derniereEntite) { this.derniereEntite = derniereEntite; }
}
