package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneBdsDamancomDTO {

    private String numeroImmatriculationCnss; // 9 chiffres
    private String cin;
    private String nom;
    private String prenom;
    private int joursTravailles = 26; // max 26 jours par mois
    private BigDecimal salaireBrutReel = BigDecimal.ZERO;
    private BigDecimal salairePlafonneCnss = BigDecimal.ZERO; // Plafond 6000 MAD / mois
    private BigDecimal cotisationSalarialeCnss = BigDecimal.ZERO; // 4.48% sur plafonné
    private BigDecimal cotisationSalarialeAmo = BigDecimal.ZERO;  // 2.26% sur brut réel

    public LigneBdsDamancomDTO() {}

    public String getNumeroImmatriculationCnss() { return numeroImmatriculationCnss; }
    public void setNumeroImmatriculationCnss(String numeroImmatriculationCnss) { this.numeroImmatriculationCnss = numeroImmatriculationCnss; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public int getJoursTravailles() { return joursTravailles; }
    public void setJoursTravailles(int joursTravailles) { this.joursTravailles = joursTravailles; }

    public BigDecimal getSalaireBrutReel() { return salaireBrutReel; }
    public void setSalaireBrutReel(BigDecimal salaireBrutReel) { this.salaireBrutReel = salaireBrutReel; }

    public BigDecimal getSalairePlafonneCnss() { return salairePlafonneCnss; }
    public void setSalairePlafonneCnss(BigDecimal salairePlafonneCnss) { this.salairePlafonneCnss = salairePlafonneCnss; }

    public BigDecimal getCotisationSalarialeCnss() { return cotisationSalarialeCnss; }
    public void setCotisationSalarialeCnss(BigDecimal cotisationSalarialeCnss) { this.cotisationSalarialeCnss = cotisationSalarialeCnss; }

    public BigDecimal getCotisationSalarialeAmo() { return cotisationSalarialeAmo; }
    public void setCotisationSalarialeAmo(BigDecimal cotisationSalarialeAmo) { this.cotisationSalarialeAmo = cotisationSalarialeAmo; }
}
