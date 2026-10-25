package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutCommandeClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Critères de recherche multi-critères pour les commandes client avec pagination côté serveur.
 */
public class CommandeClientSearchCriteria {
    private String numeroCommande;
    private String numeroCommandeExterne;
    private String bcClient;
    private String referenceClient;
    private String clientNom;
    private String clientTelephone;
    private Long clientId;
    private StatutCommandeClient statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long societeId;
    private Boolean inclureAnnulees;

    public CommandeClientSearchCriteria() {}

    public Boolean getInclureAnnulees() { return inclureAnnulees; }
    public void setInclureAnnulees(Boolean inclureAnnulees) { this.inclureAnnulees = inclureAnnulees; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public String getNumeroCommandeExterne() {
        if (numeroCommandeExterne != null && !numeroCommandeExterne.trim().isEmpty()) {
            return numeroCommandeExterne;
        }
        if (bcClient != null && !bcClient.trim().isEmpty()) {
            return bcClient;
        }
        return referenceClient;
    }
    public void setNumeroCommandeExterne(String numeroCommandeExterne) {
        this.numeroCommandeExterne = numeroCommandeExterne;
    }

    public String getBcClient() {
        return getNumeroCommandeExterne();
    }
    public void setBcClient(String bcClient) {
        this.bcClient = bcClient;
        if (this.numeroCommandeExterne == null) {
            this.numeroCommandeExterne = bcClient;
        }
    }

    public String getReferenceClient() {
        return getNumeroCommandeExterne();
    }
    public void setReferenceClient(String referenceClient) {
        this.referenceClient = referenceClient;
        if (this.numeroCommandeExterne == null) {
            this.numeroCommandeExterne = referenceClient;
        }
    }

    public String getBcClientExterne() {
        return getNumeroCommandeExterne();
    }
    public void setBcClientExterne(String bcClientExterne) {
        setBcClient(bcClientExterne);
    }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public StatutCommandeClient getStatut() { return statut; }
    public void setStatut(StatutCommandeClient statut) { this.statut = statut; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
