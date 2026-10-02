package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutDevis;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche multi-critères pour les devis avec pagination côté serveur.
 */
public class DevisSearchCriteria {
    private String numeroDevis;
    private Long clientId;
    private String clientNom;
    private StatutDevis statut;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long societeId;

    public DevisSearchCriteria() {}

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public StatutDevis getStatut() { return statut; }
    public void setStatut(StatutDevis statut) { this.statut = statut; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
