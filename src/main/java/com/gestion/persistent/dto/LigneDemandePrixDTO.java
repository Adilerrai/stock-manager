package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneDemandePrixDTO {
    private Long id;
    private Long demandePrixId;
    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantiteDemandee;
    private String unite;

    public LigneDemandePrixDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandePrixId() { return demandePrixId; }
    public void setDemandePrixId(Long demandePrixId) { this.demandePrixId = demandePrixId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }
}
