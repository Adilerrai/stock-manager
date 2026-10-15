package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneDepenseConsommableDTO {

    private Long id;
    private Long depenseId;
    private Long consommableId;
    private String consommableReference;
    private String consommableDesignation;
    private String uniteMesure;
    private BigDecimal quantite;
    private BigDecimal prixUnitaire;
    private BigDecimal montantTotal;
    private String notes;

    public LigneDepenseConsommableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDepenseId() { return depenseId; }
    public void setDepenseId(Long depenseId) { this.depenseId = depenseId; }

    public Long getConsommableId() { return consommableId; }
    public void setConsommableId(Long consommableId) { this.consommableId = consommableId; }

    public String getConsommableReference() { return consommableReference; }
    public void setConsommableReference(String consommableReference) { this.consommableReference = consommableReference; }

    public String getConsommableDesignation() { return consommableDesignation; }
    public void setConsommableDesignation(String consommableDesignation) { this.consommableDesignation = consommableDesignation; }

    public String getUniteMesure() { return uniteMesure; }
    public void setUniteMesure(String uniteMesure) { this.uniteMesure = uniteMesure; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
