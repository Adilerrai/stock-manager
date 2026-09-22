package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ligne du Relevé de déduction de TVA selon l'Article 112 du CGI Maroc.
 */
public class LigneReleveDeductionDTO {

    private Integer numOrdre;
    private String numeroFacture;
    private LocalDate dateFacture;
    private String nomFournisseur;
    private String identifiantFiscal;
    private String ice;
    private String designation;
    private BigDecimal montantHt = BigDecimal.ZERO;
    private BigDecimal tauxTva = BigDecimal.valueOf(20.00);
    private BigDecimal montantTva = BigDecimal.ZERO;
    private BigDecimal montantTtc = BigDecimal.ZERO;
    private BigDecimal prorata = BigDecimal.valueOf(100.00);
    private BigDecimal montantDeductible = BigDecimal.ZERO;
    private String modePaiement;
    private LocalDate datePaiement;
    private String compte;
    private String statutRapprochement = "NON_RAPPROCHE"; // RAPPROCHE ou NON_RAPPROCHE
    private LocalDateTime dateRapprochement;
    private String referenceBancaire;

    public LigneReleveDeductionDTO() {}

    public Integer getNumOrdre() { return numOrdre; }
    public void setNumOrdre(Integer numOrdre) { this.numOrdre = numOrdre; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDate getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDate dateFacture) { this.dateFacture = dateFacture; }

    public String getNomFournisseur() { return nomFournisseur; }
    public void setNomFournisseur(String nomFournisseur) { this.nomFournisseur = nomFournisseur; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getMontantHt() { return montantHt; }
    public void setMontantHt(BigDecimal montantHt) { this.montantHt = montantHt != null ? montantHt : BigDecimal.ZERO; }

    public BigDecimal getTauxTva() { return tauxTva; }
    public void setTauxTva(BigDecimal tauxTva) { this.tauxTva = tauxTva != null ? tauxTva : BigDecimal.ZERO; }

    public BigDecimal getMontantTva() { return montantTva; }
    public void setMontantTva(BigDecimal montantTva) { this.montantTva = montantTva != null ? montantTva : BigDecimal.ZERO; }

    public BigDecimal getMontantTtc() { return montantTtc; }
    public void setMontantTtc(BigDecimal montantTtc) { this.montantTtc = montantTtc != null ? montantTtc : BigDecimal.ZERO; }

    public BigDecimal getProrata() { return prorata; }
    public void setProrata(BigDecimal prorata) { this.prorata = prorata != null ? prorata : BigDecimal.valueOf(100.00); }

    public BigDecimal getMontantDeductible() { return montantDeductible; }
    public void setMontantDeductible(BigDecimal montantDeductible) { this.montantDeductible = montantDeductible != null ? montantDeductible : BigDecimal.ZERO; }

    public String getModePaiement() { return modePaiement; }
    public void setModePaiement(String modePaiement) { this.modePaiement = modePaiement; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public String getCompte() { return compte; }
    public void setCompte(String compte) { this.compte = compte; }

    public String getStatutRapprochement() { return statutRapprochement; }
    public void setStatutRapprochement(String statutRapprochement) { this.statutRapprochement = statutRapprochement; }

    public LocalDateTime getDateRapprochement() { return dateRapprochement; }
    public void setDateRapprochement(LocalDateTime dateRapprochement) { this.dateRapprochement = dateRapprochement; }

    public String getReferenceBancaire() { return referenceBancaire; }
    public void setReferenceBancaire(String referenceBancaire) { this.referenceBancaire = referenceBancaire; }
}
