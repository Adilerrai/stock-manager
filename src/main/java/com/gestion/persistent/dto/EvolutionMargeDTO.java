package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class EvolutionMargeDTO {
    private String moisKey; // ex: "2026-04"
    private String labelMois; // ex: "Avril"
    private int annee;
    private int moisNumero;

    private BigDecimal caBrutHT = BigDecimal.ZERO;
    private BigDecimal remisesHT = BigDecimal.ZERO;
    private BigDecimal avoirsHT = BigDecimal.ZERO;
    private BigDecimal caNetHT = BigDecimal.ZERO;
    private BigDecimal coutVentesHT = BigDecimal.ZERO;
    private BigDecimal margeCommerciale = BigDecimal.ZERO;
    private BigDecimal tauxMargePct = BigDecimal.ZERO;

    public EvolutionMargeDTO() {}

    public EvolutionMargeDTO(String moisKey, String labelMois, int annee, int moisNumero,
                            BigDecimal caBrutHT, BigDecimal remisesHT, BigDecimal avoirsHT,
                            BigDecimal caNetHT, BigDecimal coutVentesHT,
                            BigDecimal margeCommerciale, BigDecimal tauxMargePct) {
        this.moisKey = moisKey;
        this.labelMois = labelMois;
        this.annee = annee;
        this.moisNumero = moisNumero;
        this.caBrutHT = caBrutHT != null ? caBrutHT : BigDecimal.ZERO;
        this.remisesHT = remisesHT != null ? remisesHT : BigDecimal.ZERO;
        this.avoirsHT = avoirsHT != null ? avoirsHT : BigDecimal.ZERO;
        this.caNetHT = caNetHT != null ? caNetHT : BigDecimal.ZERO;
        this.coutVentesHT = coutVentesHT != null ? coutVentesHT : BigDecimal.ZERO;
        this.margeCommerciale = margeCommerciale != null ? margeCommerciale : BigDecimal.ZERO;
        this.tauxMargePct = tauxMargePct != null ? tauxMargePct : BigDecimal.ZERO;
    }

    public String getMoisKey() { return moisKey; }
    public void setMoisKey(String moisKey) { this.moisKey = moisKey; }

    public String getLabelMois() { return labelMois; }
    public void setLabelMois(String labelMois) { this.labelMois = labelMois; }

    public int getAnnee() { return annee; }
    public void setAnnee(int annee) { this.annee = annee; }

    public int getMoisNumero() { return moisNumero; }
    public void setMoisNumero(int moisNumero) { this.moisNumero = moisNumero; }

    public BigDecimal getCaBrutHT() { return caBrutHT; }
    public void setCaBrutHT(BigDecimal caBrutHT) { this.caBrutHT = caBrutHT; }

    public BigDecimal getRemisesHT() { return remisesHT; }
    public void setRemisesHT(BigDecimal remisesHT) { this.remisesHT = remisesHT; }

    public BigDecimal getAvoirsHT() { return avoirsHT; }
    public void setAvoirsHT(BigDecimal avoirsHT) { this.avoirsHT = avoirsHT; }

    public BigDecimal getCaNetHT() { return caNetHT; }
    public void setCaNetHT(BigDecimal caNetHT) { this.caNetHT = caNetHT; }

    public BigDecimal getCoutVentesHT() { return coutVentesHT; }
    public void setCoutVentesHT(BigDecimal coutVentesHT) { this.coutVentesHT = coutVentesHT; }

    public BigDecimal getMargeCommerciale() { return margeCommerciale; }
    public void setMargeCommerciale(BigDecimal margeCommerciale) { this.margeCommerciale = margeCommerciale; }

    public BigDecimal getTauxMargePct() { return tauxMargePct; }
    public void setTauxMargePct(BigDecimal tauxMargePct) { this.tauxMargePct = tauxMargePct; }
}
