package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.enums.ModePaiement;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DepenseSearchCriteria {
    private String reference;
    private String designation;
    private String beneficiaire;
    private CategorieDepense categorie;
    private ModePaiement modePaiement;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;
    private Long fournisseurId;
    private Boolean deverseeCompta;
    private Long societeId;

    public DepenseSearchCriteria() {}

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public CategorieDepense getCategorie() { return categorie; }
    public void setCategorie(CategorieDepense categorie) { this.categorie = categorie; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public Boolean getDeverseeCompta() { return deverseeCompta; }
    public void setDeverseeCompta(Boolean deverseeCompta) { this.deverseeCompta = deverseeCompta; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
