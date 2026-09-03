package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class PerformanceCommercialDTO {
    private Long commercialId;
    private String nomCommercial;
    private String email;
    private String telephone;

    // Objectifs & Réalisations CA
    private BigDecimal caRealise = BigDecimal.ZERO;
    private BigDecimal objectifCA = BigDecimal.ZERO;
    private BigDecimal tauxRealisationCA = BigDecimal.ZERO; // %

    // Objectifs & Réalisations Marges
    private BigDecimal margeRealisee = BigDecimal.ZERO;
    private BigDecimal objectifMarge = BigDecimal.ZERO;
    private BigDecimal tauxRealisationMarge = BigDecimal.ZERO; // %

    // Activité commerciale
    private Long nombreVentes = 0L;
    private Long nombreClientsPortefeuille = 0L;
    private Long nouveauxClientsPeriode = 0L;

    // Risque & Recouvrement
    private BigDecimal totalImpayesClients = BigDecimal.ZERO;

    public PerformanceCommercialDTO() {}

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public String getNomCommercial() { return nomCommercial; }
    public void setNomCommercial(String nomCommercial) { this.nomCommercial = nomCommercial; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public BigDecimal getCaRealise() { return caRealise; }
    public void setCaRealise(BigDecimal caRealise) { this.caRealise = caRealise; }

    public BigDecimal getObjectifCA() { return objectifCA; }
    public void setObjectifCA(BigDecimal objectifCA) { this.objectifCA = objectifCA; }

    public BigDecimal getTauxRealisationCA() { return tauxRealisationCA; }
    public void setTauxRealisationCA(BigDecimal tauxRealisationCA) { this.tauxRealisationCA = tauxRealisationCA; }

    public BigDecimal getMargeRealisee() { return margeRealisee; }
    public void setMargeRealisee(BigDecimal margeRealisee) { this.margeRealisee = margeRealisee; }

    public BigDecimal getObjectifMarge() { return objectifMarge; }
    public void setObjectifMarge(BigDecimal objectifMarge) { this.objectifMarge = objectifMarge; }

    public BigDecimal getTauxRealisationMarge() { return tauxRealisationMarge; }
    public void setTauxRealisationMarge(BigDecimal tauxRealisationMarge) { this.tauxRealisationMarge = tauxRealisationMarge; }

    public Long getNombreVentes() { return nombreVentes; }
    public void setNombreVentes(Long nombreVentes) { this.nombreVentes = nombreVentes; }

    public Long getNombreClientsPortefeuille() { return nombreClientsPortefeuille; }
    public void setNombreClientsPortefeuille(Long nombreClientsPortefeuille) { this.nombreClientsPortefeuille = nombreClientsPortefeuille; }

    public Long getNouveauxClientsPeriode() { return nouveauxClientsPeriode; }
    public void setNouveauxClientsPeriode(Long nouveauxClientsPeriode) { this.nouveauxClientsPeriode = nouveauxClientsPeriode; }

    public BigDecimal getTotalImpayesClients() { return totalImpayesClients; }
    public void setTotalImpayesClients(BigDecimal totalImpayesClients) { this.totalImpayesClients = totalImpayesClients; }
}
