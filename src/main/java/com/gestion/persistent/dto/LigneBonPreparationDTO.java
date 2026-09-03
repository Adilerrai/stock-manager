package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneBonPreparationDTO {
    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitNom;
    private BigDecimal quantiteCommandee;
    private BigDecimal quantitePreparee;
    private String emplacementDepot;
    private String statutLigne;

    public LigneBonPreparationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public BigDecimal getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(BigDecimal quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public BigDecimal getQuantitePreparee() { return quantitePreparee; }
    public void setQuantitePreparee(BigDecimal quantitePreparee) { this.quantitePreparee = quantitePreparee; }

    public String getEmplacementDepot() { return emplacementDepot; }
    public void setEmplacementDepot(String emplacementDepot) { this.emplacementDepot = emplacementDepot; }

    public String getStatutLigne() { return statutLigne; }
    public void setStatutLigne(String statutLigne) { this.statutLigne = statutLigne; }
}
