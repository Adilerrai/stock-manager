package com.gestion.persistent.dto;

import com.gestion.persistent.enums.QualiteProduit;
import com.gestion.persistent.enums.TypeMouvement;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MouvementStockDTO {
    private Long id;
    private Long produitId;
    private String produitLibelle;
    private String produitReference;
    private TypeMouvement typeMouvement;
    private BigDecimal quantite;
    private BigDecimal quantiteAvant;
    private BigDecimal quantiteApres;
    private String referenceDocument;
    private String motif;
    private LocalDateTime dateMouvement;
    private String utilisateur;
    private QualiteProduit qualiteProduit;
    private Long depotId;
    private String depotNom;

    public MouvementStockDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getProduitLibelle() { return produitLibelle; }
    public void setProduitLibelle(String produitLibelle) { this.produitLibelle = produitLibelle; }

    public String getProduitReference() { return produitReference; }
    public void setProduitReference(String produitReference) { this.produitReference = produitReference; }

    public TypeMouvement getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(TypeMouvement typeMouvement) { this.typeMouvement = typeMouvement; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public BigDecimal getQuantiteAvant() { return quantiteAvant; }
    public void setQuantiteAvant(BigDecimal quantiteAvant) { this.quantiteAvant = quantiteAvant; }

    public BigDecimal getQuantiteApres() { return quantiteApres; }
    public void setQuantiteApres(BigDecimal quantiteApres) { this.quantiteApres = quantiteApres; }

    public String getReferenceDocument() { return referenceDocument; }
    public void setReferenceDocument(String referenceDocument) { this.referenceDocument = referenceDocument; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getUtilisateur() { return utilisateur; }
    public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }

    public QualiteProduit getQualiteProduit() { return qualiteProduit; }
    public void setQualiteProduit(QualiteProduit qualiteProduit) { this.qualiteProduit = qualiteProduit; }

    public Long getDepotId() { return depotId; }
    public void setDepotId(Long depotId) { this.depotId = depotId; }

    public String getDepotNom() { return depotNom; }
    public void setDepotNom(String depotNom) { this.depotNom = depotNom; }
}

