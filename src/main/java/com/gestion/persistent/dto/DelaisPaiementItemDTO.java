package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DelaisPaiementItemDTO {

    private String typeFlux; // "ACHAT" ou "VENTE"
    private Long pieceId;
    private String numeroFacture;
    private LocalDate dateFacture;
    private LocalDate dateEcheanceLegale;
    private LocalDate datePaiementEffective;

    // Tiers (Fournisseur ou Client)
    private Long tiersId;
    private String tiersNom;
    private String tiersIce;
    private String tiersIdentifiantFiscal;

    // Montants
    private BigDecimal montantHt = BigDecimal.ZERO;
    private BigDecimal montantTva = BigDecimal.ZERO;
    private BigDecimal montantTtc = BigDecimal.ZERO;
    private BigDecimal montantPaye = BigDecimal.ZERO;
    private BigDecimal montantRestant = BigDecimal.ZERO;

    // Délais Loi 69-21
    private int delaiApplicableJours; // 60 jours ou contractuel <= 120
    private long joursRetard;
    private int moisRetard;
    private BigDecimal tauxAmendePourcentage = BigDecimal.ZERO; // ex: 3.00, 3.85, 4.70...
    private BigDecimal montantAmende = BigDecimal.ZERO;

    private String statutDelai; // "DANS_LES_DELAIS", "A_RISQUE_15J", "EN_RETARD_IMPAYE", "REGLE_EN_RETARD"

    public DelaisPaiementItemDTO() {}

    public String getTypeFlux() { return typeFlux; }
    public void setTypeFlux(String typeFlux) { this.typeFlux = typeFlux; }

    public Long getPieceId() { return pieceId; }
    public void setPieceId(Long pieceId) { this.pieceId = pieceId; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDate getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDate dateFacture) { this.dateFacture = dateFacture; }

    public LocalDate getDateEcheanceLegale() { return dateEcheanceLegale; }
    public void setDateEcheanceLegale(LocalDate dateEcheanceLegale) { this.dateEcheanceLegale = dateEcheanceLegale; }

    public LocalDate getDatePaiementEffective() { return datePaiementEffective; }
    public void setDatePaiementEffective(LocalDate datePaiementEffective) { this.datePaiementEffective = datePaiementEffective; }

    public Long getTiersId() { return tiersId; }
    public void setTiersId(Long tiersId) { this.tiersId = tiersId; }

    public String getTiersNom() { return tiersNom; }
    public void setTiersNom(String tiersNom) { this.tiersNom = tiersNom; }

    public String getTiersIce() { return tiersIce; }
    public void setTiersIce(String tiersIce) { this.tiersIce = tiersIce; }

    public String getTiersIdentifiantFiscal() { return tiersIdentifiantFiscal; }
    public void setTiersIdentifiantFiscal(String tiersIdentifiantFiscal) { this.tiersIdentifiantFiscal = tiersIdentifiantFiscal; }

    public BigDecimal getMontantHt() { return montantHt; }
    public void setMontantHt(BigDecimal montantHt) { this.montantHt = montantHt; }

    public BigDecimal getMontantTva() { return montantTva; }
    public void setMontantTva(BigDecimal montantTva) { this.montantTva = montantTva; }

    public BigDecimal getMontantTtc() { return montantTtc; }
    public void setMontantTtc(BigDecimal montantTtc) { this.montantTtc = montantTtc; }

    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }

    public BigDecimal getMontantRestant() { return montantRestant; }
    public void setMontantRestant(BigDecimal montantRestant) { this.montantRestant = montantRestant; }

    public int getDelaiApplicableJours() { return delaiApplicableJours; }
    public void setDelaiApplicableJours(int delaiApplicableJours) { this.delaiApplicableJours = delaiApplicableJours; }

    public long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(long joursRetard) { this.joursRetard = joursRetard; }

    public int getMoisRetard() { return moisRetard; }
    public void setMoisRetard(int moisRetard) { this.moisRetard = moisRetard; }

    public BigDecimal getTauxAmendePourcentage() { return tauxAmendePourcentage; }
    public void setTauxAmendePourcentage(BigDecimal tauxAmendePourcentage) { this.tauxAmendePourcentage = tauxAmendePourcentage; }

    public BigDecimal getMontantAmende() { return montantAmende; }
    public void setMontantAmende(BigDecimal montantAmende) { this.montantAmende = montantAmende; }

    public String getStatutDelai() { return statutDelai; }
    public void setStatutDelai(String statutDelai) { this.statutDelai = statutDelai; }
}
