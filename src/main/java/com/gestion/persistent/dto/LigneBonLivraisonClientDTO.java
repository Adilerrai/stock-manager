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
    private BigDecimal prixVenteBrut;
    private BigDecimal remisePourcentage;
    private BigDecimal remiseMontant;
    private BigDecimal montantLigne;
    private BigDecimal prixVenteMin;

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

    public BigDecimal getPrixVenteBrut() { return prixVenteBrut != null ? prixVenteBrut : prixVente; }
    public void setPrixVenteBrut(BigDecimal prixVenteBrut) { this.prixVenteBrut = prixVenteBrut; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage != null ? remisePourcentage : BigDecimal.ZERO; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getRemiseMontant() { return remiseMontant != null ? remiseMontant : BigDecimal.ZERO; }
    public void setRemiseMontant(BigDecimal remiseMontant) { this.remiseMontant = remiseMontant; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public BigDecimal getPrixVenteMin() { return prixVenteMin; }
    public void setPrixVenteMin(BigDecimal prixVenteMin) { this.prixVenteMin = prixVenteMin; }
}
