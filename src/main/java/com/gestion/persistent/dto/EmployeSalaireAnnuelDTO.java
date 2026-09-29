package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeSalaireAnnuelDTO {

    private String matricule;
    private String nom;
    private String prenom;
    private String cin;
    private String numeroCnss;
    private LocalDate dateRecrutement;
    private LocalDate dateDepart;
    private int joursTravailles = 312; // 26 jours * 12 mois
    private String situationFamiliale = "CELIBATAIRE";
    private int nombrePersonnesACharge = 0;

    // Rémunérations brutes
    private BigDecimal salaireBrutAnnuel = BigDecimal.ZERO;
    private BigDecimal avantagesEnNature = BigDecimal.ZERO;
    private BigDecimal indemnitesExonerees = BigDecimal.ZERO; // Transport / Panier dans les plafonds
    private BigDecimal totalBrutGlobal = BigDecimal.ZERO;

    // Déductions sociales
    private BigDecimal cotisationsCnssSalariales = BigDecimal.ZERO; // 4.48% (plafonné à 6000 MAD/mois)
    private BigDecimal cotisationsAmoSalariales = BigDecimal.ZERO;  // 2.26%
    private BigDecimal cotisationsCimrSalariales = BigDecimal.ZERO; // Retraite CIMR
    private BigDecimal totalRetenuesSociales = BigDecimal.ZERO;

    // Frais professionnels (35% plafonné à 35 000 MAD / an)
    private BigDecimal fraisProfessionnels = BigDecimal.ZERO;

    // Détermination IR
    private BigDecimal salaireNetImposable = BigDecimal.ZERO;
    private BigDecimal irBrut = BigDecimal.ZERO;
    private BigDecimal deductionsChargesFamille = BigDecimal.ZERO; // 360 MAD par an par personne, max 2 160 MAD
    private BigDecimal irNetRetenu = BigDecimal.ZERO;

    public EmployeSalaireAnnuelDTO() {}

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public String getNumeroCnss() { return numeroCnss; }
    public void setNumeroCnss(String numeroCnss) { this.numeroCnss = numeroCnss; }

    public LocalDate getDateRecrutement() { return dateRecrutement; }
    public void setDateRecrutement(LocalDate dateRecrutement) { this.dateRecrutement = dateRecrutement; }

    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

    public int getJoursTravailles() { return joursTravailles; }
    public void setJoursTravailles(int joursTravailles) { this.joursTravailles = joursTravailles; }

    public String getSituationFamiliale() { return situationFamiliale; }
    public void setSituationFamiliale(String situationFamiliale) { this.situationFamiliale = situationFamiliale; }

    public int getNombrePersonnesACharge() { return nombrePersonnesACharge; }
    public void setNombrePersonnesACharge(int nombrePersonnesACharge) { this.nombrePersonnesACharge = nombrePersonnesACharge; }

    public BigDecimal getSalaireBrutAnnuel() { return salaireBrutAnnuel; }
    public void setSalaireBrutAnnuel(BigDecimal salaireBrutAnnuel) { this.salaireBrutAnnuel = salaireBrutAnnuel; }

    public BigDecimal getAvantagesEnNature() { return avantagesEnNature; }
    public void setAvantagesEnNature(BigDecimal avantagesEnNature) { this.avantagesEnNature = avantagesEnNature; }

    public BigDecimal getIndemnitesExonerees() { return indemnitesExonerees; }
    public void setIndemnitesExonerees(BigDecimal indemnitesExonerees) { this.indemnitesExonerees = indemnitesExonerees; }

    public BigDecimal getTotalBrutGlobal() { return totalBrutGlobal; }
    public void setTotalBrutGlobal(BigDecimal totalBrutGlobal) { this.totalBrutGlobal = totalBrutGlobal; }

    public BigDecimal getCotisationsCnssSalariales() { return cotisationsCnssSalariales; }
    public void setCotisationsCnssSalariales(BigDecimal cotisationsCnssSalariales) { this.cotisationsCnssSalariales = cotisationsCnssSalariales; }

    public BigDecimal getCotisationsAmoSalariales() { return cotisationsAmoSalariales; }
    public void setCotisationsAmoSalariales(BigDecimal cotisationsAmoSalariales) { this.cotisationsAmoSalariales = cotisationsAmoSalariales; }

    public BigDecimal getCotisationsCimrSalariales() { return cotisationsCimrSalariales; }
    public void setCotisationsCimrSalariales(BigDecimal cotisationsCimrSalariales) { this.cotisationsCimrSalariales = cotisationsCimrSalariales; }

    public BigDecimal getTotalRetenuesSociales() { return totalRetenuesSociales; }
    public void setTotalRetenuesSociales(BigDecimal totalRetenuesSociales) { this.totalRetenuesSociales = totalRetenuesSociales; }

    public BigDecimal getFraisProfessionnels() { return fraisProfessionnels; }
    public void setFraisProfessionnels(BigDecimal fraisProfessionnels) { this.fraisProfessionnels = fraisProfessionnels; }

    public BigDecimal getSalaireNetImposable() { return salaireNetImposable; }
    public void setSalaireNetImposable(BigDecimal salaireNetImposable) { this.salaireNetImposable = salaireNetImposable; }

    public BigDecimal getIrBrut() { return irBrut; }
    public void setIrBrut(BigDecimal irBrut) { this.irBrut = irBrut; }

    public BigDecimal getDeductionsChargesFamille() { return deductionsChargesFamille; }
    public void setDeductionsChargesFamille(BigDecimal deductionsChargesFamille) { this.deductionsChargesFamille = deductionsChargesFamille; }

    public BigDecimal getIrNetRetenu() { return irNetRetenu; }
    public void setIrNetRetenu(BigDecimal irNetRetenu) { this.irNetRetenu = irNetRetenu; }
}
