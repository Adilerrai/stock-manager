package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class StockSearchCriteria {
    private String query;
    private Long produitId;
    private String produitNom;
    private String produitReference;
    private Long categorieId;
    private String statut; // "OK", "ALERTE", "RUPTURE", "ALL"
    private BigDecimal quantiteMin;
    private BigDecimal quantiteMax;
    private Long societeId;

    public StockSearchCriteria() {}

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitNom() { return produitNom; }
    public void setProduitNom(String produitNom) { this.produitNom = produitNom; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public BigDecimal getQuantiteMin() { return quantiteMin; }
    public void setQuantiteMin(BigDecimal quantiteMin) { this.quantiteMin = quantiteMin; }

    public BigDecimal getQuantiteMax() { return quantiteMax; }
    public void setQuantiteMax(BigDecimal quantiteMax) { this.quantiteMax = quantiteMax; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
