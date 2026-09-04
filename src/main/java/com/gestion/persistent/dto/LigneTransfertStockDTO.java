package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneTransfertStockDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitDesignation;
    private String produitReference;
    private BigDecimal quantiteDemandee;
    private BigDecimal quantiteExpediee;
    private BigDecimal quantiteRecue;
    private BigDecimal quantite;
    private String notes;

    public LigneTransfertStockDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitDesignation() { return produitDesignation; }
    public void setProduitDesignation(String produitDesignation) { this.produitDesignation = produitDesignation; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public BigDecimal getQuantiteExpediee() { return quantiteExpediee; }
    public void setQuantiteExpediee(BigDecimal quantiteExpediee) { this.quantiteExpediee = quantiteExpediee; }

    public BigDecimal getQuantiteRecue() { return quantiteRecue; }
    public void setQuantiteRecue(BigDecimal quantiteRecue) { this.quantiteRecue = quantiteRecue; }

    public BigDecimal getQuantite() {
        return quantite != null ? quantite : quantiteDemandee;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
        if (this.quantiteDemandee == null) {
            this.quantiteDemandee = quantite;
        }
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
