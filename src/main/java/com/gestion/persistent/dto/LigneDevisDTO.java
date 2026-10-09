package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LigneDevisDTO {
    private Long id;
    private Long produitId;
    private String produitReference;
    private String produitDesignation;
    private BigDecimal quantite;
    private BigDecimal prixUnitaireHT;
    private BigDecimal tauxTVA;
    private BigDecimal tauxRemise;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantFinal;
    private String description;

    public LigneDevisDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public String getProduitDesignation() { return produitDesignation; }
    public void setProduitDesignation(String produitDesignation) { this.produitDesignation = produitDesignation; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getTauxRemise() { return tauxRemise; }
    public void setTauxRemise(BigDecimal tauxRemise) { this.tauxRemise = tauxRemise; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public BigDecimal getMontantFinal() {
        return (montantFinal != null) ? montantFinal : montantTTC;
    }
    public void setMontantFinal(BigDecimal montantFinal) {
        this.montantFinal = montantFinal;
        if (this.montantTTC == null) {
            this.montantTTC = montantFinal;
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    // Support des objets imbriqués pour Jackson
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
            if (map.get("designation") != null && this.produitDesignation == null) {
                this.produitDesignation = map.get("designation").toString();
            } else if (map.get("nom") != null && this.produitDesignation == null) {
                this.produitDesignation = map.get("nom").toString();
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

    // Aliases pour compatibilité frontend et Jackson
    public String getDesignation() {
        return produitDesignation != null ? produitDesignation : description;
    }
    public void setDesignation(String designation) {
        if (this.produitDesignation == null) {
            this.produitDesignation = designation;
        }
        if (this.description == null) {
            this.description = designation;
        }
    }

    public String getReference() {
        return produitReference;
    }
    public void setReference(String reference) {
        this.produitReference = reference;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaireHT;
    }
    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaireHT = prixUnitaire;
    }

    public void setPrix(BigDecimal prix) {
        if (this.prixUnitaireHT == null) {
            this.prixUnitaireHT = prix;
        }
    }

    public BigDecimal getTauxTva() {
        return tauxTVA;
    }
    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTVA = tauxTva;
    }

    public BigDecimal getRemisePct() {
        return tauxRemise;
    }
    public void setRemisePct(BigDecimal remisePct) {
        this.tauxRemise = remisePct;
    }

    public void setRemise(BigDecimal remise) {
        if (this.tauxRemise == null) {
            this.tauxRemise = remise;
        }
    }

    public BigDecimal getMontantTotal() {
        return (montantFinal != null) ? montantFinal : montantTTC;
    }
    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTTC = montantTotal;
        if (this.montantFinal == null) {
            this.montantFinal = montantTotal;
        }
    }

    public String getProduitNom() {
        return getDesignation();
    }

    @JsonProperty("produit")
    public void unpackProduit(Object produit) {
        if (produit instanceof Map<?, ?> map) {
            Object idVal = map.get("id");
            if (idVal != null) {
                try {
                    this.produitId = Long.valueOf(idVal.toString());
                } catch (Exception ignored) {}
            }
            if (this.produitDesignation == null) {
                if (map.get("designation") != null) {
                    this.produitDesignation = map.get("designation").toString();
                } else if (map.get("nom") != null) {
                    this.produitDesignation = map.get("nom").toString();
                }
            }
            if (this.produitReference == null && map.get("reference") != null) {
                this.produitReference = map.get("reference").toString();
            }
        } else if (produit instanceof Number num) {
            this.produitId = num.longValue();
        } else if (produit instanceof String str && !str.trim().isEmpty()) {
            try {
                this.produitId = Long.valueOf(str.trim());
            } catch (Exception ignored) {}
        }
    }

    @JsonProperty("produit")
    public Map<String, Object> getProduit() {
        if (this.produitId == null && this.produitDesignation == null) {
            return null;
        }
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.produitId);
        map.put("designation", getDesignation());
        map.put("nom", getDesignation());
        map.put("reference", this.produitReference);
        return map;
    }
}
