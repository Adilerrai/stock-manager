package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneBonLivraisonClientDTO {
    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitDesignation;
    private BigDecimal quantiteLivree;
    private Long depotId;
    private String depotNom;
    private Long lotId;
    private String numeroLot;
    private BigDecimal prixVente;

    public LigneBonLivraisonClientDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitDesignation() { return produitDesignation; }
    public void setProduitDesignation(String produitDesignation) { this.produitDesignation = produitDesignation; }

    public BigDecimal getQuantiteLivree() { return quantiteLivree; }
    public void setQuantiteLivree(BigDecimal quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public Long getDepotId() { return depotId; }
    public void setDepotId(Long depotId) { this.depotId = depotId; }

    public String getDepotNom() { return depotNom; }
    public void setDepotNom(String depotNom) { this.depotNom = depotNom; }

    public Long getLotId() { return lotId; }
    public void setLotId(Long lotId) { this.lotId = lotId; }

    public String getNumeroLot() { return numeroLot; }
    public void setNumeroLot(String numeroLot) { this.numeroLot = numeroLot; }

    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }
}
