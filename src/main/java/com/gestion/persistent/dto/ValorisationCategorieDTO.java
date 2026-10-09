package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class ValorisationCategorieDTO {
    private Long categorieId;
    private String nomCategorie;
    private long nombreReferences = 0;
    private BigDecimal valeurStock = BigDecimal.ZERO;
    private BigDecimal pourcentageDuStockTotal = BigDecimal.ZERO;

    public ValorisationCategorieDTO() {}

    public ValorisationCategorieDTO(Long categorieId, String nomCategorie, long nombreReferences, BigDecimal valeurStock, BigDecimal pourcentageDuStockTotal) {
        this.categorieId = categorieId;
        this.nomCategorie = nomCategorie;
        this.nombreReferences = nombreReferences;
        this.valeurStock = valeurStock != null ? valeurStock : BigDecimal.ZERO;
        this.pourcentageDuStockTotal = pourcentageDuStockTotal != null ? pourcentageDuStockTotal : BigDecimal.ZERO;
    }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    public long getNombreReferences() { return nombreReferences; }
    public void setNombreReferences(long nombreReferences) { this.nombreReferences = nombreReferences; }

    public BigDecimal getValeurStock() { return valeurStock; }
    public void setValeurStock(BigDecimal valeurStock) { this.valeurStock = valeurStock; }

    public BigDecimal getPourcentageDuStockTotal() { return pourcentageDuStockTotal; }
    public void setPourcentageDuStockTotal(BigDecimal pourcentageDuStockTotal) { this.pourcentageDuStockTotal = pourcentageDuStockTotal; }
}
