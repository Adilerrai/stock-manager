package com.gestion.persistent.dto;

import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;
import java.math.BigDecimal;
import java.time.LocalDate;

public class ChequeEffetSearchCriteria {
    private String numeroPiece;
    private TypeEffet type;
    private SensEffet sens;
    private StatutEffet statut;
    private String tireur;
    private String banque;
    private LocalDate dateEcheanceDebut;
    private LocalDate dateEcheanceFin;
    private BigDecimal montantMin;
    private BigDecimal montantMax;

    public ChequeEffetSearchCriteria() {}

    public String getNumeroPiece() { return numeroPiece; }
    public void setNumeroPiece(String numeroPiece) { this.numeroPiece = numeroPiece; }

    public TypeEffet getType() { return type; }
    public void setType(TypeEffet type) { this.type = type; }

    public SensEffet getSens() { return sens; }
    public void setSens(SensEffet sens) { this.sens = sens; }

    public StatutEffet getStatut() { return statut; }
    public void setStatut(StatutEffet statut) { this.statut = statut; }

    public String getTireur() { return tireur; }
    public void setTireur(String tireur) { this.tireur = tireur; }

    public String getBanque() { return banque; }
    public void setBanque(String banque) { this.banque = banque; }

    public LocalDate getDateEcheanceDebut() { return dateEcheanceDebut; }
    public void setDateEcheanceDebut(LocalDate dateEcheanceDebut) { this.dateEcheanceDebut = dateEcheanceDebut; }

    public LocalDate getDateEcheanceFin() { return dateEcheanceFin; }
    public void setDateEcheanceFin(LocalDate dateEcheanceFin) { this.dateEcheanceFin = dateEcheanceFin; }

    public BigDecimal getMontantMin() { return montantMin; }
    public void setMontantMin(BigDecimal montantMin) { this.montantMin = montantMin; }

    public BigDecimal getMontantMax() { return montantMax; }
    public void setMontantMax(BigDecimal montantMax) { this.montantMax = montantMax; }
}
