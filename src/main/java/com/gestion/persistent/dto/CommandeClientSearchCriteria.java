package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutCommandeClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Critères de recherche multi-critères pour les commandes client avec pagination côté serveur.
 */
public class CommandeClientSearchCriteria {
    private String numeroCommande;
    private String clientNom;
    private String clientTelephone;
    private Long clientId;
    private StatutCommandeClient statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long societeId;

    public CommandeClientSearchCriteria() {}

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

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
