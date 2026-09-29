package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeclarationDelaisPaiementDTO {

    private int annee;
    private int trimestre; // 1, 2, 3, 4
    private LocalDate dateDebutPeriode;
    private LocalDate dateFinPeriode;
    private LocalDate dateLimiteDepot; // Fin du mois suivant le trimestre

    // Informations Déclarant
    private String raisonSociale;
    private String identifiantFiscal;
    private String ice;
    private String registreCommerce;

    // Totaux Globaux
    private int totalFacturesConcernees = 0;
    private int totalFacturesDansLesDelais = 0;
    private int totalFacturesEnRetard = 0;
    private BigDecimal montantTotalTtcFactures = BigDecimal.ZERO;
    private BigDecimal montantTotalEnRetard = BigDecimal.ZERO;
    private BigDecimal totalAmendesDgiExigibles = BigDecimal.ZERO;

    // Ventilation par Tranche d'Ancienneté de Retard
    // Tranche 1 (1 à 30 jours - Taux 3%)
    private int nbFacturesTranche1 = 0;
    private BigDecimal montantTranche1 = BigDecimal.ZERO;
    private BigDecimal amendesTranche1 = BigDecimal.ZERO;

    // Tranche 2 (31 à 60 jours - Taux 3.85%)
    private int nbFacturesTranche2 = 0;
    private BigDecimal montantTranche2 = BigDecimal.ZERO;
    private BigDecimal amendesTranche2 = BigDecimal.ZERO;

    // Tranche 3 (61 à 90 jours - Taux 4.70%)
    private int nbFacturesTranche3 = 0;
    private BigDecimal montantTranche3 = BigDecimal.ZERO;
    private BigDecimal amendesTranche3 = BigDecimal.ZERO;

    // Tranche 4 (> 90 jours - Taux > 4.70%)
    private int nbFacturesTranche4 = 0;
    private BigDecimal montantTranche4 = BigDecimal.ZERO;
    private BigDecimal amendesTranche4 = BigDecimal.ZERO;

    // Liste des lignes de détail
    private List<DelaisPaiementItemDTO> factures = new ArrayList<>();

    public DeclarationDelaisPaiementDTO() {}

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public int getTrimestre() { return trimestre; }
    public void setTrimestre(int trimestre) { this.trimestre = trimestre; }

    public LocalDate getDateDebutPeriode() { return dateDebutPeriode; }
    public void setDateDebutPeriode(LocalDate dateDebutPeriode) { this.dateDebutPeriode = dateDebutPeriode; }

    public LocalDate getDateFinPeriode() { return dateFinPeriode; }
    public void setDateFinPeriode(LocalDate dateFinPeriode) { this.dateFinPeriode = dateFinPeriode; }

    public LocalDate getDateLimiteDepot() { return dateLimiteDepot; }
    public void setDateLimiteDepot(LocalDate dateLimiteDepot) { this.dateLimiteDepot = dateLimiteDepot; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getRegistreCommerce() { return registreCommerce; }
    public void setRegistreCommerce(String registreCommerce) { this.registreCommerce = registreCommerce; }

    public int getTotalFacturesConcernees() { return totalFacturesConcernees; }
    public void setTotalFacturesConcernees(int totalFacturesConcernees) { this.totalFacturesConcernees = totalFacturesConcernees; }

    public int getTotalFacturesDansLesDelais() { return totalFacturesDansLesDelais; }
    public void setTotalFacturesDansLesDelais(int totalFacturesDansLesDelais) { this.totalFacturesDansLesDelais = totalFacturesDansLesDelais; }

    public int getTotalFacturesEnRetard() { return totalFacturesEnRetard; }
    public void setTotalFacturesEnRetard(int totalFacturesEnRetard) { this.totalFacturesEnRetard = totalFacturesEnRetard; }

    public BigDecimal getMontantTotalTtcFactures() { return montantTotalTtcFactures; }
    public void setMontantTotalTtcFactures(BigDecimal montantTotalTtcFactures) { this.montantTotalTtcFactures = montantTotalTtcFactures; }

    public BigDecimal getMontantTotalEnRetard() { return montantTotalEnRetard; }
    public void setMontantTotalEnRetard(BigDecimal montantTotalEnRetard) { this.montantTotalEnRetard = montantTotalEnRetard; }

    public BigDecimal getTotalAmendesDgiExigibles() { return totalAmendesDgiExigibles; }
    public void setTotalAmendesDgiExigibles(BigDecimal totalAmendesDgiExigibles) { this.totalAmendesDgiExigibles = totalAmendesDgiExigibles; }

    public int getNbFacturesTranche1() { return nbFacturesTranche1; }
    public void setNbFacturesTranche1(int nbFacturesTranche1) { this.nbFacturesTranche1 = nbFacturesTranche1; }

    public BigDecimal getMontantTranche1() { return montantTranche1; }
    public void setMontantTranche1(BigDecimal montantTranche1) { this.montantTranche1 = montantTranche1; }

    public BigDecimal getAmendesTranche1() { return amendesTranche1; }
    public void setAmendesTranche1(BigDecimal amendesTranche1) { this.amendesTranche1 = amendesTranche1; }

    public int getNbFacturesTranche2() { return nbFacturesTranche2; }
    public void setNbFacturesTranche2(int nbFacturesTranche2) { this.nbFacturesTranche2 = nbFacturesTranche2; }

    public BigDecimal getMontantTranche2() { return montantTranche2; }
    public void setMontantTranche2(BigDecimal montantTranche2) { this.montantTranche2 = montantTranche2; }

    public BigDecimal getAmendesTranche2() { return amendesTranche2; }
    public void setAmendesTranche2(BigDecimal amendesTranche2) { this.amendesTranche2 = amendesTranche2; }

    public int getNbFacturesTranche3() { return nbFacturesTranche3; }
    public void setNbFacturesTranche3(int nbFacturesTranche3) { this.nbFacturesTranche3 = nbFacturesTranche3; }

    public BigDecimal getMontantTranche3() { return montantTranche3; }
    public void setMontantTranche3(BigDecimal montantTranche3) { this.montantTranche3 = montantTranche3; }

    public BigDecimal getAmendesTranche3() { return amendesTranche3; }
    public void setAmendesTranche3(BigDecimal amendesTranche3) { this.amendesTranche3 = amendesTranche3; }

    public int getNbFacturesTranche4() { return nbFacturesTranche4; }
    public void setNbFacturesTranche4(int nbFacturesTranche4) { this.nbFacturesTranche4 = nbFacturesTranche4; }

    public BigDecimal getMontantTranche4() { return montantTranche4; }
    public void setMontantTranche4(BigDecimal montantTranche4) { this.montantTranche4 = montantTranche4; }

    public BigDecimal getAmendesTranche4() { return amendesTranche4; }
    public void setAmendesTranche4(BigDecimal amendesTranche4) { this.amendesTranche4 = amendesTranche4; }

    public List<DelaisPaiementItemDTO> getFactures() { return factures; }
    public void setFactures(List<DelaisPaiementItemDTO> factures) { this.factures = factures; }
}
