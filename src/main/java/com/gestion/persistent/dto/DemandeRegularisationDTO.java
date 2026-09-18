package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DemandeRegularisationDTO {

    private String type; // CCA, PCA, PROVISION_CLIENT
    private LocalDate dateEcriture; // Par défaut 31/12 de l'exercice
    private String compteChargeProduit; // Ex: "61310000" (Loyer), "71110000" (Marchandises)
    private String libelle;
    private BigDecimal montant;
    private Boolean extourneAutomatique = true;
    private LocalDate dateExtourne; // Par défaut 01/01 de l'exercice suivant

    public DemandeRegularisationDTO() {}

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDate getDateEcriture() { return dateEcriture; }
    public void setDateEcriture(LocalDate dateEcriture) { this.dateEcriture = dateEcriture; }

    public String getCompteChargeProduit() { return compteChargeProduit; }
    public void setCompteChargeProduit(String compteChargeProduit) { this.compteChargeProduit = compteChargeProduit; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public Boolean getExtourneAutomatique() { return extourneAutomatique; }
    public void setExtourneAutomatique(Boolean extourneAutomatique) { this.extourneAutomatique = extourneAutomatique; }

    public LocalDate getDateExtourne() { return dateExtourne; }
    public void setDateExtourne(LocalDate dateExtourne) { this.dateExtourne = dateExtourne; }
}
