package com.gestion.persistent.dto;

import com.gestion.persistent.enums.QualiteProduit;

import java.math.BigDecimal;

public class LigneCommandeDTO {
    private Long id;
    private Long commandeId;
    private Long produitId;
    private String produitDescription;
    private String designation;
    private String produitReference;
    private Integer quantiteCommandee;
    private Integer quantiteLivree;
    private BigDecimal prixUnitaire;
    private BigDecimal prixUnitaireHT;
    private BigDecimal tauxTVA;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal montantLigne;
    private QualiteProduit qualiteProduit;

    // Logistique (saisie)
    private BigDecimal poidsTotalKg;
    private BigDecimal volumeTotalM3;
    private String codeSh;
    private BigDecimal tauxDroitDouane;

    // Coût de revient (calculé, lecture seule)
    private BigDecimal prixUnitaireMad;
    private BigDecimal fraisAllouesMad;
    private BigDecimal coutRevientUnitaireMad;
    private BigDecimal coutRevientTotalMad;

    // Constructors
    public LigneCommandeDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitDescription() { return produitDescription; }
    public void setProduitDescription(String produitDescription) { this.produitDescription = produitDescription; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public QualiteProduit getQualiteProduit() {
        return qualiteProduit;
    }

    public void setQualiteProduit(QualiteProduit qualiteProduit) {
        this.qualiteProduit = qualiteProduit;
    }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public Integer getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(Integer quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }
    public Integer getQuantiteLivree() { return quantiteLivree; }
    public void setQuantiteLivree(Integer quantiteLivree) { this.quantiteLivree = quantiteLivree; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public BigDecimal getPoidsTotalKg() { return poidsTotalKg; }
    public void setPoidsTotalKg(BigDecimal poidsTotalKg) { this.poidsTotalKg = poidsTotalKg; }

    public BigDecimal getVolumeTotalM3() { return volumeTotalM3; }
    public void setVolumeTotalM3(BigDecimal volumeTotalM3) { this.volumeTotalM3 = volumeTotalM3; }

    public String getCodeSh() { return codeSh; }
    public void setCodeSh(String codeSh) { this.codeSh = codeSh; }

    public BigDecimal getTauxDroitDouane() { return tauxDroitDouane; }
    public void setTauxDroitDouane(BigDecimal tauxDroitDouane) { this.tauxDroitDouane = tauxDroitDouane; }

    public BigDecimal getPrixUnitaireMad() { return prixUnitaireMad; }
    public void setPrixUnitaireMad(BigDecimal prixUnitaireMad) { this.prixUnitaireMad = prixUnitaireMad; }

    public BigDecimal getFraisAllouesMad() { return fraisAllouesMad; }
    public void setFraisAllouesMad(BigDecimal fraisAllouesMad) { this.fraisAllouesMad = fraisAllouesMad; }

    public BigDecimal getCoutRevientUnitaireMad() { return coutRevientUnitaireMad; }
    public void setCoutRevientUnitaireMad(BigDecimal coutRevientUnitaireMad) { this.coutRevientUnitaireMad = coutRevientUnitaireMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }
}

