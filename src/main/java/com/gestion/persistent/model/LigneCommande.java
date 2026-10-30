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

    @Column(name = "prix_unitaire", precision = 14, scale = 4, nullable = false)
    private BigDecimal prixUnitaire;

    @Column(name = "prix_unitaire_ht", precision = 14, scale = 4)
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

    // ===== Logistique / coût de revient =====

    @Column(name = "prix_unitaire_devise", precision = 14, scale = 4)
    private BigDecimal prixUnitaireDevise;

    @Column(name = "prix_unitaire_mad", precision = 14, scale = 4)
    private BigDecimal prixUnitaireMad;

    @Column(name = "poids_total_kg", precision = 12, scale = 3)
    private BigDecimal poidsTotalKg;

    @Column(name = "volume_total_m3", precision = 12, scale = 4)
    private BigDecimal volumeTotalM3;

    @Column(name = "code_sh", length = 12)
    private String codeSh;

    @Column(name = "taux_droit_douane", precision = 6, scale = 2)
    private BigDecimal tauxDroitDouane;

    @Column(name = "frais_alloues_mad", precision = 15, scale = 2)
    private BigDecimal fraisAllouesMad = BigDecimal.ZERO;

    @Column(name = "cout_revient_unitaire_mad", precision = 14, scale = 4)
    private BigDecimal coutRevientUnitaireMad;

    @Column(name = "cout_revient_total_mad", precision = 15, scale = 2)
    private BigDecimal coutRevientTotalMad;

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

    public BigDecimal getPrixUnitaireDevise() { return prixUnitaireDevise; }
    public void setPrixUnitaireDevise(BigDecimal prixUnitaireDevise) { this.prixUnitaireDevise = prixUnitaireDevise; }

    public BigDecimal getPrixUnitaireMad() { return prixUnitaireMad; }
    public void setPrixUnitaireMad(BigDecimal prixUnitaireMad) { this.prixUnitaireMad = prixUnitaireMad; }

    public BigDecimal getPoidsTotalKg() { return poidsTotalKg; }
    public void setPoidsTotalKg(BigDecimal poidsTotalKg) { this.poidsTotalKg = poidsTotalKg; }

    public BigDecimal getVolumeTotalM3() { return volumeTotalM3; }
    public void setVolumeTotalM3(BigDecimal volumeTotalM3) { this.volumeTotalM3 = volumeTotalM3; }

    public String getCodeSh() { return codeSh; }
    public void setCodeSh(String codeSh) { this.codeSh = codeSh; }

    public BigDecimal getTauxDroitDouane() { return tauxDroitDouane; }
    public void setTauxDroitDouane(BigDecimal tauxDroitDouane) { this.tauxDroitDouane = tauxDroitDouane; }

    public BigDecimal getFraisAllouesMad() { return fraisAllouesMad; }
    public void setFraisAllouesMad(BigDecimal fraisAllouesMad) { this.fraisAllouesMad = fraisAllouesMad; }

    public BigDecimal getCoutRevientUnitaireMad() { return coutRevientUnitaireMad; }
    public void setCoutRevientUnitaireMad(BigDecimal coutRevientUnitaireMad) { this.coutRevientUnitaireMad = coutRevientUnitaireMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }


}
