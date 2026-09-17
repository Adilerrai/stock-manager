package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class VentilationTvaDTO {
    private int taux;
    private BigDecimal baseHT = BigDecimal.ZERO;
    private BigDecimal montantTva = BigDecimal.ZERO;
    private String description;

    public VentilationTvaDTO() {}

    public VentilationTvaDTO(int taux, BigDecimal baseHT, BigDecimal montantTva, String description) {
        this.taux = taux;
        this.baseHT = baseHT != null ? baseHT : BigDecimal.ZERO;
        this.montantTva = montantTva != null ? montantTva : BigDecimal.ZERO;
        this.description = description;
    }

    public int getTaux() {
        return taux;
    }

    public void setTaux(int taux) {
        this.taux = taux;
    }

    public BigDecimal getBaseHT() {
        return baseHT;
    }

    public void setBaseHT(BigDecimal baseHT) {
        this.baseHT = baseHT;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public void setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
