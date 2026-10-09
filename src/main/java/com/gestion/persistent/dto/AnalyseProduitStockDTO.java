package com.gestion.persistent.dto;

import com.gestion.persistent.enums.ClasseAbc;
import com.gestion.persistent.enums.StatutComportementStock;
import com.gestion.persistent.enums.VitesseRotation;
import java.math.BigDecimal;

public class AnalyseProduitStockDTO {
    private Long produitId;
    private String reference;
    private String nom;
    private String categorieNom;
    private Long categorieId;
    
    // Niveaux de stocks
    private BigDecimal quantiteStock = BigDecimal.ZERO;
    private BigDecimal seuilAlerte = BigDecimal.ZERO;
    private BigDecimal prixPmp = BigDecimal.ZERO;
    private BigDecimal valeurStock = BigDecimal.ZERO;
    private BigDecimal pourcentageValeurTotale = BigDecimal.ZERO;

    // Vitesse & consommation
    private BigDecimal consommationJournaliereMoyenne = BigDecimal.ZERO;
    private BigDecimal sortiesPeriode = BigDecimal.ZERO;
    private Integer couvertureJours; // null si stock 0, 999 si dormant
    private BigDecimal tauxRotationAnnuelle = BigDecimal.ZERO;

    // Décisionnel
    private StatutComportementStock statutComportement = StatutComportementStock.NORMAL;
    private ClasseAbc classeAbc = ClasseAbc.C;
    private VitesseRotation vitesseRotation = VitesseRotation.MOYENNE;
    private String croisementAbcRotation; // ex: "A/Rapide", "A/Lente"

    // Recommandation achat
    private boolean alerteReappro = false;
    private BigDecimal quantiteReapproConseillee = BigDecimal.ZERO;

    public AnalyseProduitStockDTO() {}

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCategorieNom() { return categorieNom; }
    public void setCategorieNom(String categorieNom) { this.categorieNom = categorieNom; }

    public Long getCategorieId() { return categorieId; }
    public void setCategorieId(Long categorieId) { this.categorieId = categorieId; }

    public BigDecimal getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(BigDecimal quantiteStock) { this.quantiteStock = quantiteStock; }

    public BigDecimal getSeuilAlerte() { return seuilAlerte; }
    public void setSeuilAlerte(BigDecimal seuilAlerte) { this.seuilAlerte = seuilAlerte; }

    public BigDecimal getPrixPmp() { return prixPmp; }
    public void setPrixPmp(BigDecimal prixPmp) { this.prixPmp = prixPmp; }

    public BigDecimal getValeurStock() { return valeurStock; }
    public void setValeurStock(BigDecimal valeurStock) { this.valeurStock = valeurStock; }

    public BigDecimal getPourcentageValeurTotale() { return pourcentageValeurTotale; }
    public void setPourcentageValeurTotale(BigDecimal pourcentageValeurTotale) { this.pourcentageValeurTotale = pourcentageValeurTotale; }

    public BigDecimal getConsommationJournaliereMoyenne() { return consommationJournaliereMoyenne; }
    public void setConsommationJournaliereMoyenne(BigDecimal consommationJournaliereMoyenne) { this.consommationJournaliereMoyenne = consommationJournaliereMoyenne; }

    public BigDecimal getSortiesPeriode() { return sortiesPeriode; }
    public void setSortiesPeriode(BigDecimal sortiesPeriode) { this.sortiesPeriode = sortiesPeriode; }

    public Integer getCouvertureJours() { return couvertureJours; }
    public void setCouvertureJours(Integer couvertureJours) { this.couvertureJours = couvertureJours; }

    public BigDecimal getTauxRotationAnnuelle() { return tauxRotationAnnuelle; }
    public void setTauxRotationAnnuelle(BigDecimal tauxRotationAnnuelle) { this.tauxRotationAnnuelle = tauxRotationAnnuelle; }

    public StatutComportementStock getStatutComportement() { return statutComportement; }
    public void setStatutComportement(StatutComportementStock statutComportement) { this.statutComportement = statutComportement; }

    public ClasseAbc getClasseAbc() { return classeAbc; }
    public void setClasseAbc(ClasseAbc classeAbc) { this.classeAbc = classeAbc; }

    public VitesseRotation getVitesseRotation() { return vitesseRotation; }
    public void setVitesseRotation(VitesseRotation vitesseRotation) { this.vitesseRotation = vitesseRotation; }

    public String getCroisementAbcRotation() { return croisementAbcRotation; }
    public void setCroisementAbcRotation(String croisementAbcRotation) { this.croisementAbcRotation = croisementAbcRotation; }

    public boolean isAlerteReappro() { return alerteReappro; }
    public void setAlerteReappro(boolean alerteReappro) { this.alerteReappro = alerteReappro; }

    public BigDecimal getQuantiteReapproConseillee() { return quantiteReapproConseillee; }
    public void setQuantiteReapproConseillee(BigDecimal quantiteReapproConseillee) { this.quantiteReapproConseillee = quantiteReapproConseillee; }
}
