package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneModeleEcritureDTO {

    private String numeroCompte; // ex: "61310000", "34552000", "44110000"
    private String libelleLigne;
    private String sens; // "DEBIT" ou "CREDIT"
    private String typeCalcul; // "BASE_HT", "TVA_SUR_HT", "BASE_TTC", "RETENUE_SOURCE", "SOLDE_CONTREPARTIE"
    private BigDecimal tauxOuCoefficient = BigDecimal.ZERO; // ex: 20.00 pour TVA, 10.00 pour RAS honoraires

    public LigneModeleEcritureDTO() {}

    public LigneModeleEcritureDTO(String numeroCompte, String libelleLigne, String sens, String typeCalcul, BigDecimal tauxOuCoefficient) {
        this.numeroCompte = numeroCompte;
        this.libelleLigne = libelleLigne;
        this.sens = sens;
        this.typeCalcul = typeCalcul;
        this.tauxOuCoefficient = tauxOuCoefficient;
    }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelleLigne() { return libelleLigne; }
    public void setLibelleLigne(String libelleLigne) { this.libelleLigne = libelleLigne; }

    public String getSens() { return sens; }
    public void setSens(String sens) { this.sens = sens; }

    public String getTypeCalcul() { return typeCalcul; }
    public void setTypeCalcul(String typeCalcul) { this.typeCalcul = typeCalcul; }

    public BigDecimal getTauxOuCoefficient() { return tauxOuCoefficient; }
    public void setTauxOuCoefficient(BigDecimal tauxOuCoefficient) { this.tauxOuCoefficient = tauxOuCoefficient; }
}
