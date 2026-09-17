package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CloturePreviewDTO {
    private Long exerciceId;
    private String exerciceCode;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private int nombreEcrituresBrouillons;
    private int nombreEcrituresValidees;

    private BigDecimal totalDebitBalance = BigDecimal.ZERO;
    private BigDecimal totalCreditBalance = BigDecimal.ZERO;
    private BigDecimal ecartBalance = BigDecimal.ZERO;

    private BigDecimal totalProduitsClasse7 = BigDecimal.ZERO;
    private BigDecimal totalChargesClasse6 = BigDecimal.ZERO;
    private BigDecimal resultatNetEstime = BigDecimal.ZERO;
    private String typeResultat; // BENEFICE ou PERTE
    private String compteResultat; // 1191 ou 1199

    private boolean eligible = true;
    private List<String> motifsIneligibilite = new ArrayList<>();

    public CloturePreviewDTO() {}

    public Long getExerciceId() {
        return exerciceId;
    }

    public void setExerciceId(Long exerciceId) {
        this.exerciceId = exerciceId;
    }

    public String getExerciceCode() {
        return exerciceCode;
    }

    public void setExerciceCode(String exerciceCode) {
        this.exerciceCode = exerciceCode;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public int getNombreEcrituresBrouillons() {
        return nombreEcrituresBrouillons;
    }

    public void setNombreEcrituresBrouillons(int nombreEcrituresBrouillons) {
        this.nombreEcrituresBrouillons = nombreEcrituresBrouillons;
    }

    public int getNombreEcrituresValidees() {
        return nombreEcrituresValidees;
    }

    public void setNombreEcrituresValidees(int nombreEcrituresValidees) {
        this.nombreEcrituresValidees = nombreEcrituresValidees;
    }

    public BigDecimal getTotalDebitBalance() {
        return totalDebitBalance;
    }

    public void setTotalDebitBalance(BigDecimal totalDebitBalance) {
        this.totalDebitBalance = totalDebitBalance;
    }

    public BigDecimal getTotalCreditBalance() {
        return totalCreditBalance;
    }

    public void setTotalCreditBalance(BigDecimal totalCreditBalance) {
        this.totalCreditBalance = totalCreditBalance;
    }

    public BigDecimal getEcartBalance() {
        return ecartBalance;
    }

    public void setEcartBalance(BigDecimal ecartBalance) {
        this.ecartBalance = ecartBalance;
    }

    public BigDecimal getTotalProduitsClasse7() {
        return totalProduitsClasse7;
    }

    public void setTotalProduitsClasse7(BigDecimal totalProduitsClasse7) {
        this.totalProduitsClasse7 = totalProduitsClasse7;
    }

    public BigDecimal getTotalChargesClasse6() {
        return totalChargesClasse6;
    }

    public void setTotalChargesClasse6(BigDecimal totalChargesClasse6) {
        this.totalChargesClasse6 = totalChargesClasse6;
    }

    public BigDecimal getResultatNetEstime() {
        return resultatNetEstime;
    }

    public void setResultatNetEstime(BigDecimal resultatNetEstime) {
        this.resultatNetEstime = resultatNetEstime;
    }

    public String getTypeResultat() {
        return typeResultat;
    }

    public void setTypeResultat(String typeResultat) {
        this.typeResultat = typeResultat;
    }

    public String getCompteResultat() {
        return compteResultat;
    }

    public void setCompteResultat(String compteResultat) {
        this.compteResultat = compteResultat;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getMotifsIneligibilite() {
        return motifsIneligibilite;
    }

    public void setMotifsIneligibilite(List<String> motifsIneligibilite) {
        this.motifsIneligibilite = motifsIneligibilite;
    }
}
