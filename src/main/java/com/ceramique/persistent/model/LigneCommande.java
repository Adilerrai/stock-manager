package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.QualiteProduit;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande")
public class LigneCommande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(name = "prix_unitaire", precision = 10, scale = 2, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(name = "montant_ligne", precision = 10, scale = 2, nullable = false)
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


    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public QualiteProduit getQualiteProduit() { return qualiteProduit; }
    public void setQualiteProduit(QualiteProduit qualiteProduit) { this.qualiteProduit = qualiteProduit; }


}