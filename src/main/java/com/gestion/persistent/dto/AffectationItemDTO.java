package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class AffectationItemDTO {
    private Long factureId;
    private BigDecimal montant;

    public AffectationItemDTO() {}

    public AffectationItemDTO(Long factureId, BigDecimal montant) {
        this.factureId = factureId;
        this.montant = montant;
    }

    public Long getFactureId() {
        return factureId;
    }

    public void setFactureId(Long factureId) {
        this.factureId = factureId;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }
}
