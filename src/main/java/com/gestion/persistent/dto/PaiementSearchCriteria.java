package com.gestion.persistent.dto;

import com.gestion.persistent.enums.ModePaiement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaiementSearchCriteria {
    private ModePaiement modePaiement;
    private Long clientId;
    private Long venteId;
    private Long factureId;
    private Boolean annule;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;

    public PaiementSearchCriteria() {}

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getVenteId() { return venteId; }
    public void setVenteId(Long venteId) { this.venteId = venteId; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public Boolean getAnnule() { return annule; }
    public void setAnnule(Boolean annule) { this.annule = annule; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }
}
