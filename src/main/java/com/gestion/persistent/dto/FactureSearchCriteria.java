package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutFacture;
import java.math.BigDecimal;
import java.time.LocalDate;

public class FactureSearchCriteria {
    private String numeroFacture;
    private Long clientId;
    private StatutFacture statut;
    private Boolean estEchue;
    private Boolean payee;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;

    public FactureSearchCriteria() {}

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public StatutFacture getStatut() { return statut; }
    public void setStatut(StatutFacture statut) { this.statut = statut; }

    public Boolean getEstEchue() { return estEchue; }
    public void setEstEchue(Boolean estEchue) { this.estEchue = estEchue; }

    public Boolean getPayee() { return payee; }
    public void setPayee(Boolean payee) { this.payee = payee; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }
}
