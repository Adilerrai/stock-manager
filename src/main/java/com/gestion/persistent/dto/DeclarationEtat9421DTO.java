package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DeclarationEtat9421DTO {

    private int exercice;
    private String raisonSociale;
    private String identifiantFiscal;
    private String ice;
    private String numeroCnssEntreprise;
    private LocalDate dateGeneration;

    // Totaux
    private int totalEmployes = 0;
    private BigDecimal totalMasseSalarialeBrute = BigDecimal.ZERO;
    private BigDecimal totalAvantages = BigDecimal.ZERO;
    private BigDecimal totalIndemnitesExonerees = BigDecimal.ZERO;
    private BigDecimal totalRetenuesSociales = BigDecimal.ZERO;
    private BigDecimal totalFraisProfessionnels = BigDecimal.ZERO;
    private BigDecimal totalNetImposable = BigDecimal.ZERO;
    private BigDecimal totalIrRetenu = BigDecimal.ZERO;

    private List<EmployeSalaireAnnuelDTO> employes = new ArrayList<>();

    public DeclarationEtat9421DTO() {
        this.dateGeneration = LocalDate.now();
    }

    public int getExercice() { return exercice; }
    public void setExercice(int exercice) { this.exercice = exercice; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getNumeroCnssEntreprise() { return numeroCnssEntreprise; }
    public void setNumeroCnssEntreprise(String numeroCnssEntreprise) { this.numeroCnssEntreprise = numeroCnssEntreprise; }

    public LocalDate getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDate dateGeneration) { this.dateGeneration = dateGeneration; }

    public int getTotalEmployes() { return totalEmployes; }
    public void setTotalEmployes(int totalEmployes) { this.totalEmployes = totalEmployes; }

    public BigDecimal getTotalMasseSalarialeBrute() { return totalMasseSalarialeBrute; }
    public void setTotalMasseSalarialeBrute(BigDecimal totalMasseSalarialeBrute) { this.totalMasseSalarialeBrute = totalMasseSalarialeBrute; }

    public BigDecimal getTotalAvantages() { return totalAvantages; }
    public void setTotalAvantages(BigDecimal totalAvantages) { this.totalAvantages = totalAvantages; }

    public BigDecimal getTotalIndemnitesExonerees() { return totalIndemnitesExonerees; }
    public void setTotalIndemnitesExonerees(BigDecimal totalIndemnitesExonerees) { this.totalIndemnitesExonerees = totalIndemnitesExonerees; }

    public BigDecimal getTotalRetenuesSociales() { return totalRetenuesSociales; }
    public void setTotalRetenuesSociales(BigDecimal totalRetenuesSociales) { this.totalRetenuesSociales = totalRetenuesSociales; }

    public BigDecimal getTotalFraisProfessionnels() { return totalFraisProfessionnels; }
    public void setTotalFraisProfessionnels(BigDecimal totalFraisProfessionnels) { this.totalFraisProfessionnels = totalFraisProfessionnels; }

    public BigDecimal getTotalNetImposable() { return totalNetImposable; }
    public void setTotalNetImposable(BigDecimal totalNetImposable) { this.totalNetImposable = totalNetImposable; }

    public BigDecimal getTotalIrRetenu() { return totalIrRetenu; }
    public void setTotalIrRetenu(BigDecimal totalIrRetenu) { this.totalIrRetenu = totalIrRetenu; }

    public List<EmployeSalaireAnnuelDTO> getEmployes() { return employes; }
    public void setEmployes(List<EmployeSalaireAnnuelDTO> employes) { this.employes = employes; }
}
