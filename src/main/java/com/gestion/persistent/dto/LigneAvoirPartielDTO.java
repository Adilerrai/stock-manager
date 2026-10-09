package com.gestion.persistent.dto;

import com.gestion.persistent.enums.MotifRetour;
import java.math.BigDecimal;

public class LigneAvoirPartielDTO {
    private Long produitId;
    private String designation;
    private BigDecimal quantite = BigDecimal.ONE;
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;
    private BigDecimal tauxTVA = BigDecimal.valueOf(20);
    private Boolean remettreEnStock = true;
    private MotifRetour motifRetour = MotifRetour.AUTRE;
    private String motif;

    public LigneAvoirPartielDTO() {}

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public Boolean getRemettreEnStock() { return remettreEnStock; }
    public void setRemettreEnStock(Boolean remettreEnStock) { this.remettreEnStock = remettreEnStock; }

    public MotifRetour getMotifRetour() { return motifRetour; }
    public void setMotifRetour(MotifRetour motifRetour) { this.motifRetour = motifRetour; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
