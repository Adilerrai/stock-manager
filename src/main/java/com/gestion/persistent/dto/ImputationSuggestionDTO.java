package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class ImputationSuggestionDTO {

    private String numeroCompteChargeProduit;
    private String libelleCompteChargeProduit;
    private String numeroCompteContrepartie; // 4411 (Fournisseur), 3421 (Client), 5141 (Banque), 5161 (Caisse)
    private String libelleCompteContrepartie;
    private String numeroCompteTva; // 34552 (TVA récup/charges), 4455 (TVA facturée)
    private String libelleCompteTva;
    private BigDecimal tauxTvaSuggere; // 20%, 14%, 10%, 7%, 0%
    private String categorieImputation;
    private double scoreConfiance; // 0.0 à 1.0 (ex: 0.95 = 95%)
    private String explication;

    public ImputationSuggestionDTO() {}

    public String getNumeroCompteChargeProduit() { return numeroCompteChargeProduit; }
    public void setNumeroCompteChargeProduit(String numeroCompteChargeProduit) { this.numeroCompteChargeProduit = numeroCompteChargeProduit; }

    public String getLibelleCompteChargeProduit() { return libelleCompteChargeProduit; }
    public void setLibelleCompteChargeProduit(String libelleCompteChargeProduit) { this.libelleCompteChargeProduit = libelleCompteChargeProduit; }

    public String getNumeroCompteContrepartie() { return numeroCompteContrepartie; }
    public void setNumeroCompteContrepartie(String numeroCompteContrepartie) { this.numeroCompteContrepartie = numeroCompteContrepartie; }

    public String getLibelleCompteContrepartie() { return libelleCompteContrepartie; }
    public void setLibelleCompteContrepartie(String libelleCompteContrepartie) { this.libelleCompteContrepartie = libelleCompteContrepartie; }

    public String getNumeroCompteTva() { return numeroCompteTva; }
    public void setNumeroCompteTva(String numeroCompteTva) { this.numeroCompteTva = numeroCompteTva; }

    public String getLibelleCompteTva() { return libelleCompteTva; }
    public void setLibelleCompteTva(String libelleCompteTva) { this.libelleCompteTva = libelleCompteTva; }

    public BigDecimal getTauxTvaSuggere() { return tauxTvaSuggere; }
    public void setTauxTvaSuggere(BigDecimal tauxTvaSuggere) { this.tauxTvaSuggere = tauxTvaSuggere; }

    public String getCategorieImputation() { return categorieImputation; }
    public void setCategorieImputation(String categorieImputation) { this.categorieImputation = categorieImputation; }

    public double getScoreConfiance() { return scoreConfiance; }
    public void setScoreConfiance(double scoreConfiance) { this.scoreConfiance = scoreConfiance; }

    public String getExplication() { return explication; }
    public void setExplication(String explication) { this.explication = explication; }
}
