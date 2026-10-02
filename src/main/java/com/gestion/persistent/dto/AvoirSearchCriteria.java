package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.TypeAvoir;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Critères de recherche multi-critères pour les avoirs avec pagination côté serveur.
 */
public class AvoirSearchCriteria {
    private String numeroAvoir;
    private TypeAvoir typeAvoir;
    private StatutAvoir statut;
    private Long clientId;
    private Long fournisseurId;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long societeId;

    public AvoirSearchCriteria() {}

    public String getNumeroAvoir() { return numeroAvoir; }
    public void setNumeroAvoir(String numeroAvoir) { this.numeroAvoir = numeroAvoir; }

    public TypeAvoir getTypeAvoir() { return typeAvoir; }
    public void setTypeAvoir(TypeAvoir typeAvoir) { this.typeAvoir = typeAvoir; }

    public StatutAvoir getStatut() { return statut; }
    public void setStatut(StatutAvoir statut) { this.statut = statut; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

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
