package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gestion.persistent.enums.CleRepartition;
import com.gestion.persistent.enums.StatutCharge;
import com.gestion.persistent.enums.TypeCharge;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Charge d'approche (fret, transitaire, douane, magasinage...) rattachée à une commande fournisseur.
 * Si ligneCommande est renseignée, la charge est directe sur cette ligne ; sinon elle est répartie
 * selon cleRepartition.
 */
@Entity
@Table(name = "charges_achat")
public class ChargeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_commande_id")
    private LigneCommande ligneCommande;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_charge", nullable = false, length = 30)
    private TypeCharge typeCharge;

    private String libelle;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id")
    private Fournisseur prestataire;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_achat_id")
    private FactureAchat factureAchat;

    @Column(name = "numero_facture_externe", length = 50)
    private String numeroFactureExterne;

    @Column(length = 10)
    private String devise = "MAD";

    @Column(name = "taux_change", precision = 12, scale = 6)
    private BigDecimal tauxChange = BigDecimal.ONE;

    @Column(name = "montant_estime_devise", precision = 15, scale = 2)
    private BigDecimal montantEstimeDevise = BigDecimal.ZERO;

    @Column(name = "montant_reel_devise", precision = 15, scale = 2)
    private BigDecimal montantReelDevise;

    @Column(name = "montant_retenu_mad", precision = 15, scale = 2)
    private BigDecimal montantRetenuMad = BigDecimal.ZERO;

    @Column(name = "montant_tva_mad", precision = 15, scale = 2)
    private BigDecimal montantTvaMad = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "cle_repartition", length = 20)
    private CleRepartition cleRepartition = CleRepartition.VALEUR;

    private Boolean incorporable = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StatutCharge statut = StatutCharge.ESTIMEE;

    @Column(name = "date_charge")
    private LocalDateTime dateCharge = LocalDateTime.now();

    @Column(name = "appliquee_au_stock")
    private Boolean appliqueeAuStock = false;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            if (tenant != null) {
                this.pointDeVenteId = tenant;
            } else if (this.commande != null && this.commande.getPointDeVenteId() != null) {
                this.pointDeVenteId = this.commande.getPointDeVenteId();
            } else {
                this.pointDeVenteId = 1L;
            }
        }
    }

    public ChargeAchat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public LigneCommande getLigneCommande() { return ligneCommande; }
    public void setLigneCommande(LigneCommande ligneCommande) { this.ligneCommande = ligneCommande; }

    public TypeCharge getTypeCharge() { return typeCharge; }
    public void setTypeCharge(TypeCharge typeCharge) { this.typeCharge = typeCharge; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public Fournisseur getPrestataire() { return prestataire; }
    public void setPrestataire(Fournisseur prestataire) { this.prestataire = prestataire; }

    public FactureAchat getFactureAchat() { return factureAchat; }
    public void setFactureAchat(FactureAchat factureAchat) { this.factureAchat = factureAchat; }

    public String getNumeroFactureExterne() { return numeroFactureExterne; }
    public void setNumeroFactureExterne(String numeroFactureExterne) { this.numeroFactureExterne = numeroFactureExterne; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public BigDecimal getTauxChange() { return tauxChange; }
    public void setTauxChange(BigDecimal tauxChange) { this.tauxChange = tauxChange; }

    public BigDecimal getMontantEstimeDevise() { return montantEstimeDevise; }
    public void setMontantEstimeDevise(BigDecimal montantEstimeDevise) { this.montantEstimeDevise = montantEstimeDevise; }

    public BigDecimal getMontantReelDevise() { return montantReelDevise; }
    public void setMontantReelDevise(BigDecimal montantReelDevise) { this.montantReelDevise = montantReelDevise; }

    public BigDecimal getMontantRetenuMad() { return montantRetenuMad; }
    public void setMontantRetenuMad(BigDecimal montantRetenuMad) { this.montantRetenuMad = montantRetenuMad; }

    public BigDecimal getMontantTvaMad() { return montantTvaMad; }
    public void setMontantTvaMad(BigDecimal montantTvaMad) { this.montantTvaMad = montantTvaMad; }

    public CleRepartition getCleRepartition() { return cleRepartition; }
    public void setCleRepartition(CleRepartition cleRepartition) { this.cleRepartition = cleRepartition; }

    public Boolean getIncorporable() { return incorporable; }
    public void setIncorporable(Boolean incorporable) { this.incorporable = incorporable; }

    public StatutCharge getStatut() { return statut; }
    public void setStatut(StatutCharge statut) { this.statut = statut; }

    public LocalDateTime getDateCharge() { return dateCharge; }
    public void setDateCharge(LocalDateTime dateCharge) { this.dateCharge = dateCharge; }

    public Boolean getAppliqueeAuStock() { return appliqueeAuStock; }
    public void setAppliqueeAuStock(Boolean appliqueeAuStock) { this.appliqueeAuStock = appliqueeAuStock; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
