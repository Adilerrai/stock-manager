package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutVente;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VenteSearchCriteria {
    private Long clientId;
    private Long caissierId;
    private StatutVente statut;
    private Boolean payee;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;

    public VenteSearchCriteria() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getCaissierId() { return caissierId; }
    public void setCaissierId(Long caissierId) { this.caissierId = caissierId; }

    public StatutVente getStatut() { return statut; }
    public void setStatut(StatutVente statut) { this.statut = statut; }

    public Boolean getPayee() { return payee; }
    public void setPayee(Boolean payee) { this.payee = payee; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }
}
