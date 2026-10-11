package com.gestion.persistent.model;

import com.gestion.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande")
public class LigneCommande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "quantite_commandee")
    private Integer quantiteCommandee;

    @Column(name = "quantite_livree")
    private Integer quantiteLivree ;

    @Column(name = "prix_unitaire", precision = 12, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(name = "prix_unitaire_ht", precision = 12, scale = 2)
    private BigDecimal prixUnitaireHT;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = BigDecimal.ZERO;

    @Column(name = "montant_ht", precision = 12, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_tva", precision = 12, scale = 2)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 2)
    private BigDecimal montantTTC;

    @Column(name = "montant_ligne", precision = 12, scale = 2, nullable = false)
    private BigDecimal montantLigne;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_produit")
    private QualiteProduit qualiteProduit;

    // Constructors, getters, setters
    public LigneCommande() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

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

    public QualiteProduit getQualiteProduit() { return qualiteProduit; }
    public void setQualiteProduit(QualiteProduit qualiteProduit) { this.qualiteProduit = qualiteProduit; }


}
