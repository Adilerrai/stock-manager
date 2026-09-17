package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeclarationTvaDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal totalVentesHT = BigDecimal.ZERO;
    private BigDecimal tvaCollectee = BigDecimal.ZERO; // Compte 4455 (Crédit)
    private BigDecimal totalAchatsHT = BigDecimal.ZERO;
    private BigDecimal tvaDeductibleCharges = BigDecimal.ZERO; // Compte 34551
    private BigDecimal tvaDeductibleImmo = BigDecimal.ZERO; // Compte 34552
    private BigDecimal tvaDeductible = BigDecimal.ZERO; // Compte 3455 (Total)
    private BigDecimal tvaAPayer = BigDecimal.ZERO;
    private BigDecimal creditTva = BigDecimal.ZERO;
    private List<VentilationTvaDTO> ventilationParTaux = new ArrayList<>();

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

    // Alias getters for frontend compatibility
    @JsonProperty("tvaNetteDue")
    public BigDecimal getTvaNetteDue() {
        return tvaAPayer != null ? tvaAPayer : BigDecimal.ZERO;
    }

    @JsonProperty("totalTvaDeductible")
    public BigDecimal getTotalTvaDeductible() {
        return tvaDeductible != null ? tvaDeductible : BigDecimal.ZERO;
    }

    @JsonProperty("creditTvaReportable")
    public BigDecimal getCreditTvaReportable() {
        return creditTva != null ? creditTva : BigDecimal.ZERO;
    }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getTotalVentesHT() { return totalVentesHT; }
    public void setTotalVentesHT(BigDecimal totalVentesHT) { this.totalVentesHT = totalVentesHT != null ? totalVentesHT : BigDecimal.ZERO; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee != null ? tvaCollectee : BigDecimal.ZERO; }

    public BigDecimal getTotalAchatsHT() { return totalAchatsHT; }
    public void setTotalAchatsHT(BigDecimal totalAchatsHT) { this.totalAchatsHT = totalAchatsHT != null ? totalAchatsHT : BigDecimal.ZERO; }

    public BigDecimal getTvaDeductibleCharges() { return tvaDeductibleCharges; }
    public void setTvaDeductibleCharges(BigDecimal tvaDeductibleCharges) { this.tvaDeductibleCharges = tvaDeductibleCharges != null ? tvaDeductibleCharges : BigDecimal.ZERO; }

    public BigDecimal getTvaDeductibleImmo() { return tvaDeductibleImmo; }
    public void setTvaDeductibleImmo(BigDecimal tvaDeductibleImmo) { this.tvaDeductibleImmo = tvaDeductibleImmo != null ? tvaDeductibleImmo : BigDecimal.ZERO; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible != null ? tvaDeductible : BigDecimal.ZERO; }

    public BigDecimal getTvaAPayer() { return tvaAPayer; }
    public void setTvaAPayer(BigDecimal tvaAPayer) { this.tvaAPayer = tvaAPayer != null ? tvaAPayer : BigDecimal.ZERO; }

    public BigDecimal getCreditTva() { return creditTva; }
    public void setCreditTva(BigDecimal creditTva) { this.creditTva = creditTva != null ? creditTva : BigDecimal.ZERO; }

    public List<VentilationTvaDTO> getVentilationParTaux() { return ventilationParTaux; }
    public void setVentilationParTaux(List<VentilationTvaDTO> ventilationParTaux) { this.ventilationParTaux = ventilationParTaux != null ? ventilationParTaux : new ArrayList<>(); }
}
