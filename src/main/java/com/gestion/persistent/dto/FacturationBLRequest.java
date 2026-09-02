package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FacturationBLRequest {
    private Long clientId;
    private List<Long> bonLivraisonIds;
    private LocalDate dateFacture;
    private LocalDate dateEcheance;
    private BigDecimal remiseGlobale;
    private String notes;
    private Long userId;

    public FacturationBLRequest() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public List<Long> getBonLivraisonIds() { return bonLivraisonIds; }
    public void setBonLivraisonIds(List<Long> bonLivraisonIds) { this.bonLivraisonIds = bonLivraisonIds; }

    public LocalDate getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDate dateFacture) { this.dateFacture = dateFacture; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public BigDecimal getRemiseGlobale() { return remiseGlobale; }
    public void setRemiseGlobale(BigDecimal remiseGlobale) { this.remiseGlobale = remiseGlobale; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
