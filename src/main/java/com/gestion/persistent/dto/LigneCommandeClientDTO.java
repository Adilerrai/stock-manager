package com.gestion.persistent.dto;

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

    private BigDecimal prixVenteMin;

    private BigDecimal quantiteCommandee;
    private BigDecimal quantiteLivree;
    private BigDecimal quantiteReliquat;
    private BigDecimal remisePourcentage;
    private BigDecimal remiseMontant;
    private Boolean annulee;
    private String motifAnnulation;

    // Constructors
    public LigneCommandeClientDTO() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getQuantiteCommandee() { return quantiteCommandee != null ? quantiteCommandee : quantite; }
    public void setQuantiteCommandee(BigDecimal quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public BigDecimal getQuantiteLivree() { return quantiteLivree != null ? quantiteLivree : BigDecimal.ZERO; }
    public void setQuantiteLivree(BigDecimal quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public BigDecimal getQuantiteReliquat() { return quantiteReliquat; }
    public void setQuantiteReliquat(BigDecimal quantiteReliquat) { this.quantiteReliquat = quantiteReliquat; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage != null ? remisePourcentage : BigDecimal.ZERO; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getRemiseMontant() { return remiseMontant != null ? remiseMontant : BigDecimal.ZERO; }
    public void setRemiseMontant(BigDecimal remiseMontant) { this.remiseMontant = remiseMontant; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public Boolean getAnnulee() { return annulee != null ? annulee : false; }
    public void setAnnulee(Boolean annulee) { this.annulee = annulee; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public BigDecimal getPrixVenteMin() { return prixVenteMin; }
    public void setPrixVenteMin(BigDecimal prixVenteMin) { this.prixVenteMin = prixVenteMin; }
}

