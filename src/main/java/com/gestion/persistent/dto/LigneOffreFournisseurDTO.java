package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneOffreFournisseurDTO {
    private Long id;
    private Long offreFournisseurId;
    private Long ligneDemandePrixId;
    private Long produitId;
    private String designation;
    private BigDecimal prixUnitaireHT;
    private BigDecimal prixUnitaireDevise;
    private String codeSh;
    private BigDecimal remisePourcentage;
    private BigDecimal tauxTVA;
    private BigDecimal quantiteProposee;
    private Boolean disponibleStock;
    private Boolean estRetenue;
    private BigDecimal montantTotalHT;

    public LigneOffreFournisseurDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOffreFournisseurId() { return offreFournisseurId; }
    public void setOffreFournisseurId(Long offreFournisseurId) { this.offreFournisseurId = offreFournisseurId; }

    public Long getLigneDemandePrixId() { return ligneDemandePrixId; }
    public void setLigneDemandePrixId(Long ligneDemandePrixId) { this.ligneDemandePrixId = ligneDemandePrixId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getQuantiteProposee() { return quantiteProposee; }
    public void setQuantiteProposee(BigDecimal quantiteProposee) { this.quantiteProposee = quantiteProposee; }

    public Boolean getDisponibleStock() { return disponibleStock; }
    public void setDisponibleStock(Boolean disponibleStock) { this.disponibleStock = disponibleStock; }

    public Boolean getEstRetenue() { return estRetenue; }
    public void setEstRetenue(Boolean estRetenue) { this.estRetenue = estRetenue; }

    public BigDecimal getPrixUnitaireDevise() { return prixUnitaireDevise; }
    public void setPrixUnitaireDevise(BigDecimal prixUnitaireDevise) { this.prixUnitaireDevise = prixUnitaireDevise; }

    public String getCodeSh() { return codeSh; }
    public void setCodeSh(String codeSh) { this.codeSh = codeSh; }

    public BigDecimal getMontantTotalHT() { return montantTotalHT; }
    public void setMontantTotalHT(BigDecimal montantTotalHT) { this.montantTotalHT = montantTotalHT; }
}
