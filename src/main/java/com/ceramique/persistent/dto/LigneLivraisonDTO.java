package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.QualiteProduit;
import com.ceramique.persistent.model.Depot;
import com.ceramique.persistent.model.Livraison;
import com.ceramique.persistent.model.Produit;
import jakarta.persistence.*;

import java.math.BigDecimal;

public class LigneLivraisonDTO {

    private Long id;
    private Long livraisonId;

    private Long quantiteLivree;

    private ProduitDTO produit;

    private BigDecimal prixProduit;

    private DepotDTO depot;
    private QualiteProduit qualiteProduit;


    // Getters and Setters
    public QualiteProduit getQualiteProduit() {
        return qualiteProduit;
    }

    public void setQualiteProduit(QualiteProduit qualiteProduit) {
        this.qualiteProduit = qualiteProduit;
    }
    // Constructors
    public LigneLivraisonDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLivraisonId() {
        return livraisonId;
    }

    public void setLivraisonId(Long livraisonId) {
        this.livraisonId = livraisonId;
    }

     public Long getQuantiteLivree() {
        return quantiteLivree;
    }

    public void setQuantiteLivree(Long quantiteLivree) {
        this.quantiteLivree = quantiteLivree;
    }

    public ProduitDTO getProduit() {
        return produit;
    }

    public void setProduit(ProduitDTO produit) {
        this.produit = produit;
    }

    public BigDecimal getPrixProduit() {
        return prixProduit;
    }

    public void setPrixProduit(BigDecimal prixProduit) {
        this.prixProduit = prixProduit;
    }

    public DepotDTO getDepot() {
        return depot;
    }

    public void setDepot(DepotDTO depot) {
        this.depot = depot;
    }
}
