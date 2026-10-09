package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneAvoiriableDTO {
    private Long ligneFactureId;
    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantiteFacturee = BigDecimal.ZERO;
    private BigDecimal quantiteDejaAvoiriee = BigDecimal.ZERO;
    private BigDecimal quantiteDisponible = BigDecimal.ZERO;
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;
    private BigDecimal tauxTVA = BigDecimal.ZERO;
    private BigDecimal montantTTCFacture = BigDecimal.ZERO;
    private boolean entierementAvoiriee = false;

    public LigneAvoiriableDTO() {}

    public Long getLigneFactureId() { return ligneFactureId; }
    public void setLigneFactureId(Long ligneFactureId) { this.ligneFactureId = ligneFactureId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantiteFacturee() { return quantiteFacturee; }
    public void setQuantiteFacturee(BigDecimal quantiteFacturee) { this.quantiteFacturee = quantiteFacturee; }

    public BigDecimal getQuantiteDejaAvoiriee() { return quantiteDejaAvoiriee; }
    public void setQuantiteDejaAvoiriee(BigDecimal quantiteDejaAvoiriee) { this.quantiteDejaAvoiriee = quantiteDejaAvoiriee; }

    public BigDecimal getQuantiteDisponible() { return quantiteDisponible; }
    public void setQuantiteDisponible(BigDecimal quantiteDisponible) { this.quantiteDisponible = quantiteDisponible; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantTTCFacture() { return montantTTCFacture; }
    public void setMontantTTCFacture(BigDecimal montantTTCFacture) { this.montantTTCFacture = montantTTCFacture; }

    public boolean isEntierementAvoiriee() { return entierementAvoiriee; }
    public void setEntierementAvoiriee(boolean entierementAvoiriee) { this.entierementAvoiriee = entierementAvoiriee; }
}
