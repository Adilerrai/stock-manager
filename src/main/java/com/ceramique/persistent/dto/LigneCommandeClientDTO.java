package com.ceramique.persistent.dto;

import java.math.BigDecimal;

public class LigneCommandeClientDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String produitReference;
    private BigDecimal quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montantLigne;
    private String observations;

    // Constructors
    public LigneCommandeClientDTO() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
