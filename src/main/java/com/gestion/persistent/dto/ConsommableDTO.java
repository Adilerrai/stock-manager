package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CategorieConsommable;
import com.gestion.persistent.enums.UniteConsommable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ConsommableDTO {

    private Long id;
    private Long pointDeVenteId;
    private String reference;
    private String codeBarre;
    private String designation;
    private String description;
    private CategorieConsommable categorie;
    private String categorieLibelle;
    private UniteConsommable uniteMesure;
    private String uniteMesureLibelle;
    private BigDecimal stockActuel;
    private BigDecimal stockMinimum;
    private BigDecimal dernierPrixAchat;
    private String emplacement;
    private Boolean actif;
    private String notes;
    private Boolean enAlerteStock;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereMiseAJour;

    public ConsommableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getCodeBarre() { return codeBarre; }
    public void setCodeBarre(String codeBarre) { this.codeBarre = codeBarre; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CategorieConsommable getCategorie() { return categorie; }
    public void setCategorie(CategorieConsommable categorie) {
        this.categorie = categorie;
        if (categorie != null) {
            this.categorieLibelle = categorie.getLibelle();
        }
    }

    public String getCategorieLibelle() { return categorieLibelle; }
    public void setCategorieLibelle(String categorieLibelle) { this.categorieLibelle = categorieLibelle; }

    public UniteConsommable getUniteMesure() { return uniteMesure; }
    public void setUniteMesure(UniteConsommable uniteMesure) {
        this.uniteMesure = uniteMesure;
        if (uniteMesure != null) {
            this.uniteMesureLibelle = uniteMesure.getLibelle();
        }
    }

    public String getUniteMesureLibelle() { return uniteMesureLibelle; }
    public void setUniteMesureLibelle(String uniteMesureLibelle) { this.uniteMesureLibelle = uniteMesureLibelle; }

    public BigDecimal getStockActuel() { return stockActuel; }
    public void setStockActuel(BigDecimal stockActuel) { this.stockActuel = stockActuel; }

    public BigDecimal getStockMinimum() { return stockMinimum; }
    public void setStockMinimum(BigDecimal stockMinimum) { this.stockMinimum = stockMinimum; }

    public BigDecimal getDernierPrixAchat() { return dernierPrixAchat; }
    public void setDernierPrixAchat(BigDecimal dernierPrixAchat) { this.dernierPrixAchat = dernierPrixAchat; }

    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Boolean getEnAlerteStock() { return enAlerteStock; }
    public void setEnAlerteStock(Boolean enAlerteStock) { this.enAlerteStock = enAlerteStock; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDerniereMiseAJour() { return derniereMiseAJour; }
    public void setDerniereMiseAJour(LocalDateTime derniereMiseAJour) { this.derniereMiseAJour = derniereMiseAJour; }
}
