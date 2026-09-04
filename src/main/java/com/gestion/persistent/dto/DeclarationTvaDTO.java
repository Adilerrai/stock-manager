package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DeclarationTvaDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal tvaCollectee = BigDecimal.ZERO; // Compte 4455 (Crédit)
    private BigDecimal tvaDeductible = BigDecimal.ZERO; // Compte 3455 (Débit)
    private BigDecimal tvaAPayer = BigDecimal.ZERO;
    private BigDecimal creditTva = BigDecimal.ZERO;

    public DeclarationTvaDTO() {}

    public DeclarationTvaDTO(LocalDate dateDebut, LocalDate dateFin, BigDecimal tvaCollectee, BigDecimal tvaDeductible) {
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.tvaCollectee = tvaCollectee != null ? tvaCollectee : BigDecimal.ZERO;
        this.tvaDeductible = tvaDeductible != null ? tvaDeductible : BigDecimal.ZERO;

        BigDecimal diff = this.tvaCollectee.subtract(this.tvaDeductible);
        if (diff.compareTo(BigDecimal.ZERO) >= 0) {
            this.tvaAPayer = diff;
            this.creditTva = BigDecimal.ZERO;
        } else {
            this.tvaAPayer = BigDecimal.ZERO;
            this.creditTva = diff.abs();
        }
    }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible; }

    public BigDecimal getTvaAPayer() { return tvaAPayer; }
    public void setTvaAPayer(BigDecimal tvaAPayer) { this.tvaAPayer = tvaAPayer; }

    public BigDecimal getCreditTva() { return creditTva; }
    public void setCreditTva(BigDecimal creditTva) { this.creditTva = creditTva; }
}
