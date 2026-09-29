package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class AssistanceSaisieKilometriqueDTO {

    // Compte saisi
    private String numeroCompteSaisi;
    private BigDecimal montantSaisi = BigDecimal.ZERO;
    private String sensSaisi; // "DEBIT" ou "CREDIT"

    // Suggestion de TVA automatique
    private boolean tvaApplicable = false;
    private String compteTvaSuggere;
    private String libelleTvaSuggere;
    private BigDecimal tauxTva = new BigDecimal("20.00");
    private BigDecimal montantTvaCalcule = BigDecimal.ZERO;
    private String sensTva; // "DEBIT" ou "CREDIT"

    // Suggestion de Contrepartie
    private String compteContrepartieSuggere;
    private String libelleContrepartieSuggere;
    private BigDecimal montantContrepartieCalcule = BigDecimal.ZERO;
    private String sensContrepartie;

    // Raccourci d'équilibrage instantané (touche * ou F9)
    private BigDecimal montantEquilibrage = BigDecimal.ZERO;
    private String sensEquilibrage; // "DEBIT" ou "CREDIT"
    private boolean dejaEquilibree = false;

    public AssistanceSaisieKilometriqueDTO() {}

    public String getNumeroCompteSaisi() { return numeroCompteSaisi; }
    public void setNumeroCompteSaisi(String numeroCompteSaisi) { this.numeroCompteSaisi = numeroCompteSaisi; }

    public BigDecimal getMontantSaisi() { return montantSaisi; }
    public void setMontantSaisi(BigDecimal montantSaisi) { this.montantSaisi = montantSaisi; }

    public String getSensSaisi() { return sensSaisi; }
    public void setSensSaisi(String sensSaisi) { this.sensSaisi = sensSaisi; }

    public boolean isTvaApplicable() { return tvaApplicable; }
    public void setTvaApplicable(boolean tvaApplicable) { this.tvaApplicable = tvaApplicable; }

    public String getCompteTvaSuggere() { return compteTvaSuggere; }
    public void setCompteTvaSuggere(String compteTvaSuggere) { this.compteTvaSuggere = compteTvaSuggere; }

    public String getLibelleTvaSuggere() { return libelleTvaSuggere; }
    public void setLibelleTvaSuggere(String libelleTvaSuggere) { this.libelleTvaSuggere = libelleTvaSuggere; }

    public BigDecimal getTauxTva() { return tauxTva; }
    public void setTauxTva(BigDecimal tauxTva) { this.tauxTva = tauxTva; }

    public BigDecimal getMontantTvaCalcule() { return montantTvaCalcule; }
    public void setMontantTvaCalcule(BigDecimal montantTvaCalcule) { this.montantTvaCalcule = montantTvaCalcule; }

    public String getSensTva() { return sensTva; }
    public void setSensTva(String sensTva) { this.sensTva = sensTva; }

    public String getCompteContrepartieSuggere() { return compteContrepartieSuggere; }
    public void setCompteContrepartieSuggere(String compteContrepartieSuggere) { this.compteContrepartieSuggere = compteContrepartieSuggere; }

    public String getLibelleContrepartieSuggere() { return libelleContrepartieSuggere; }
    public void setLibelleContrepartieSuggere(String libelleContrepartieSuggere) { this.libelleContrepartieSuggere = libelleContrepartieSuggere; }

    public BigDecimal getMontantContrepartieCalcule() { return montantContrepartieCalcule; }
    public void setMontantContrepartieCalcule(BigDecimal montantContrepartieCalcule) { this.montantContrepartieCalcule = montantContrepartieCalcule; }

    public String getSensContrepartie() { return sensContrepartie; }
    public void setSensContrepartie(String sensContrepartie) { this.sensContrepartie = sensContrepartie; }

    public BigDecimal getMontantEquilibrage() { return montantEquilibrage; }
    public void setMontantEquilibrage(BigDecimal montantEquilibrage) { this.montantEquilibrage = montantEquilibrage; }

    public String getSensEquilibrage() { return sensEquilibrage; }
    public void setSensEquilibrage(String sensEquilibrage) { this.sensEquilibrage = sensEquilibrage; }

    public boolean isDejaEquilibree() { return dejaEquilibree; }
    public void setDejaEquilibree(boolean dejaEquilibree) { this.dejaEquilibree = dejaEquilibree; }
}
