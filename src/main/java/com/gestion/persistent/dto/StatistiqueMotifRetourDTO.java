package com.gestion.persistent.dto;

import com.gestion.persistent.enums.MotifRetour;
import java.math.BigDecimal;

public class StatistiqueMotifRetourDTO {
    private MotifRetour motifRetour;
    private String motifLibelle;
    private Long nombreRetours;
    private BigDecimal montantTotalTTC;
    private BigDecimal pourcentageMontant; // %

    public StatistiqueMotifRetourDTO() {}

    public StatistiqueMotifRetourDTO(MotifRetour motifRetour, Long nombreRetours, BigDecimal montantTotalTTC) {
        this.motifRetour = motifRetour;
        this.motifLibelle = motifRetour != null ? motifRetour.getLibelle() : "Non spécifié";
        this.nombreRetours = nombreRetours != null ? nombreRetours : 0L;
        this.montantTotalTTC = montantTotalTTC != null ? montantTotalTTC : BigDecimal.ZERO;
    }

    public MotifRetour getMotifRetour() { return motifRetour; }
    public void setMotifRetour(MotifRetour motifRetour) {
        this.motifRetour = motifRetour;
        if (motifRetour != null) this.motifLibelle = motifRetour.getLibelle();
    }

    public String getMotifLibelle() { return motifLibelle; }
    public void setMotifLibelle(String motifLibelle) { this.motifLibelle = motifLibelle; }

    public Long getNombreRetours() { return nombreRetours; }
    public void setNombreRetours(Long nombreRetours) { this.nombreRetours = nombreRetours; }

    public BigDecimal getMontantTotalTTC() { return montantTotalTTC; }
    public void setMontantTotalTTC(BigDecimal montantTotalTTC) { this.montantTotalTTC = montantTotalTTC; }

    public BigDecimal getPourcentageMontant() { return pourcentageMontant; }
    public void setPourcentageMontant(BigDecimal pourcentageMontant) { this.pourcentageMontant = pourcentageMontant; }
}
