package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("produit")
    public void unpackProduit(Object produit) {
        if (produit instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) produit;
            if (map.get("id") != null) {
                try {
                    this.produitId = Long.valueOf(map.get("id").toString());
                } catch (Exception ignored) {}
            }
            if (map.get("reference") != null && this.produitReference == null) {
                this.produitReference = map.get("reference").toString();
            }
            if (map.get("designation") != null && this.produitNom == null) {
                this.produitNom = map.get("designation").toString();
            } else if (map.get("nom") != null && this.produitNom == null) {
                this.produitNom = map.get("nom").toString();
            }
        } else if (produit instanceof Number) {
            this.produitId = ((Number) produit).longValue();
        } else if (produit instanceof String) {
            try {
                this.produitId = Long.valueOf((String) produit);
            } catch (Exception ignored) {}
        }
    }

    @JsonProperty("produit_id")
    public void setProduit_id(Long id) {
        if (this.produitId == null) this.produitId = id;
    }

    @JsonProperty("idProduit")
    public void setIdProduit(Long id) {
        if (this.produitId == null) this.produitId = id;
    }

    @JsonProperty("articleId")
    public void setArticleId(Long id) {
        if (this.produitId == null) this.produitId = id;
    }

    @JsonProperty("prixUnitaireHT")
    public void setPrixUnitaireHT(BigDecimal pu) {
        if (this.prixUnitaire == null) this.prixUnitaire = pu;
    }

    @JsonProperty("prix")
    public void setPrix(BigDecimal pu) {
        if (this.prixUnitaire == null) this.prixUnitaire = pu;
    }

    @JsonProperty("tauxRemise")
    public void setTauxRemise(BigDecimal r) {
        if (this.remisePourcentage == null) this.remisePourcentage = r;
    }

    @JsonProperty("remise")
    public void setRemise(BigDecimal r) {
        if (this.remisePourcentage == null) this.remisePourcentage = r;
    }

    @JsonProperty("remisePct")
    public void setRemisePct(BigDecimal r) {
        if (this.remisePourcentage == null) this.remisePourcentage = r;
    }

    @JsonProperty("montantTotal")
    public void setMontantTotal(BigDecimal mt) {
        if (this.montantLigne == null) this.montantLigne = mt;
    }

    @JsonProperty("montantFinal")
    public void setMontantFinal(BigDecimal mf) {
        if (this.montantLigne == null) this.montantLigne = mf;
    }

    @JsonProperty("montantTTC")
    public void setMontantTTC(BigDecimal mt) {
        if (this.montantLigne == null) this.montantLigne = mt;
    }
}

