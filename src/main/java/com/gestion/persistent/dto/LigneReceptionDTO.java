package com.gestion.persistent.dto;

import com.gestion.persistent.enums.QualiteProduit;
import java.math.BigDecimal;

public class LigneReceptionDTO {
    private Long ligneCommandeId;
    private Long produitId;
    private Integer quantiteRecue;
    private QualiteProduit qualiteProduit;
    private Long depotId;
    private BigDecimal prixUnitaire;

    public LigneReceptionDTO() {}

    public LigneReceptionDTO(Long ligneCommandeId, Long produitId, Integer quantiteRecue) {
        this.ligneCommandeId = ligneCommandeId;
        this.produitId = produitId;
        this.quantiteRecue = quantiteRecue;
    }

    public Long getLigneCommandeId() {
        return ligneCommandeId;
    }

    public void setLigneCommandeId(Long ligneCommandeId) {
        this.ligneCommandeId = ligneCommandeId;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public Integer getQuantiteRecue() {
        return quantiteRecue;
    }

    public void setQuantiteRecue(Integer quantiteRecue) {
        this.quantiteRecue = quantiteRecue;
    }

    public QualiteProduit getQualiteProduit() {
        return qualiteProduit;
    }

    public void setQualiteProduit(QualiteProduit qualiteProduit) {
        this.qualiteProduit = qualiteProduit;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}
