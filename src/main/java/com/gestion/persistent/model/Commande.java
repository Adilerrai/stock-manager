package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutLivraison;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes", uniqueConstraints = {
    @UniqueConstraint(name = "uk_commandes_tenant_numero", columnNames = {"point_de_vente_id", "numero_commande"})
})
public class Commande {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_commande", nullable = false)
    private String numeroCommande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Enumerated(EnumType.STRING)
    private StatutCommande statut = StatutCommande.BROUILLON;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_livraison")
    private StatutLivraison statutLivraison = StatutLivraison.EN_ATTENTE;

    @Column(name = "date_commande")
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "date_livraison_prevue")
    private LocalDateTime dateLivraisonPrevue;

    @Column(name = "date_livraison_reelle")
    private LocalDateTime dateLivraisonReelle;

    @Column(name = "montant_total", precision = 12, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "appliquer_tva")
    private Boolean appliquerTVA = false;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = BigDecimal.ZERO;

    @Column(name = "montant_ht", precision = 12, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 12, scale = 2)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 12, scale = 2)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "devise", length = 10)
    private String devise = "MAD";

    private String observations;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LigneCommande> lignesCommande = new ArrayList<>();

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Livraison> livraisons = new ArrayList<>();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    // ===== Configuration import / international =====

    @Enumerated(EnumType.STRING)
    @Column(name = "type_achat", length = 20)
    private com.gestion.persistent.enums.TypeAchat typeAchat = com.gestion.persistent.enums.TypeAchat.NATIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoterm", length = 10)
    private com.gestion.persistent.enums.Incoterm incoterm;

    @Column(name = "lieu_incoterm", length = 100)
    private String lieuIncoterm;

    @Column(name = "port_embarquement", length = 100)
    private String portEmbarquement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieu_destination_id")
    private LieuLogistique lieuDestination;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_transport", length = 20)
    private com.gestion.persistent.enums.ModeTransport modeTransport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "condition_paiement_id")
    private ConditionPaiement conditionPaiement;

    @Column(name = "taux_change_commande", precision = 12, scale = 6)
    private BigDecimal tauxChangeCommande;

    @Column(name = "taux_change_douane", precision = 12, scale = 6)
    private BigDecimal tauxChangeDouane;

    @Column(name = "numero_bl", length = 50)
    private String numeroBl;

    @Column(name = "numero_conteneur", length = 50)
    private String numeroConteneur;

    @Column(name = "navire", length = 100)
    private String navire;

    @Column(name = "date_etd")
    private LocalDateTime dateEtd;

    @Column(name = "date_eta")
    private LocalDateTime dateEta;

    @Column(name = "numero_dum", length = 50)
    private String numeroDum;

    @Column(name = "date_dum")
    private LocalDateTime dateDum;

    // ===== Proforma Invoice (PI) =====

    @Column(name = "numero_pi", length = 100)
    private String numeroPi;

    @Column(name = "date_pi")
    private java.time.LocalDate datePi;

    @Column(name = "reference_pi", length = 200)
    private String referencePi;

    // ===== Engagement PortNet / Domiciliation Bancaire =====

    @Column(name = "numero_engagement_portnet", length = 100)
    private String numeroEngagementPortnet;

    @Column(name = "date_engagement_portnet")
    private java.time.LocalDate dateEngagementPortnet;

    @Column(name = "banque_domiciliation", length = 100)
    private String banqueDomiciliation;

    @Column(name = "agence_domiciliation", length = 100)
    private String agenceDomiciliation;

    /** Workflow : BROUILLON -> PI_RECUE -> ENGAGEMENT_VALIDE -> DOSSIER_BANQUE_ENVOYE -> FONDS_TRANSFERES */
    @Column(name = "statut_workflow_import", length = 30)
    private String statutWorkflowImport = "BROUILLON";

    @Column(name = "date_envoi_banque")
    private java.time.LocalDate dateEnvoiBanque;

    @Column(name = "numero_ordre_transfert", length = 100)
    private String numeroOrdreTransfert;

    // ===== Totaux coût de revient (calculés par CoutRevientService) =====

    @Column(name = "montant_marchandise_devise", precision = 15, scale = 2)
    private BigDecimal montantMarchandiseDevise = BigDecimal.ZERO;

    @Column(name = "montant_marchandise_mad", precision = 15, scale = 2)
    private BigDecimal montantMarchandiseMad = BigDecimal.ZERO;

    @Column(name = "valeur_douane_mad", precision = 15, scale = 2)
    private BigDecimal valeurDouaneMad;

    @Column(name = "total_charges_estimees_mad", precision = 15, scale = 2)
    private BigDecimal totalChargesEstimeesMad = BigDecimal.ZERO;

    @Column(name = "total_charges_reelles_mad", precision = 15, scale = 2)
    private BigDecimal totalChargesReellesMad = BigDecimal.ZERO;

    @Column(name = "total_charges_mad", precision = 15, scale = 2)
    private BigDecimal totalChargesMad = BigDecimal.ZERO;

    @Column(name = "cout_revient_total_mad", precision = 15, scale = 2)
    private BigDecimal coutRevientTotalMad = BigDecimal.ZERO;

    @Column(name = "coefficient_approche", precision = 8, scale = 4)
    private BigDecimal coefficientApproche;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_couts", length = 20)
    private com.gestion.persistent.enums.StatutCouts statutCouts = com.gestion.persistent.enums.StatutCouts.ESTIME;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            if (tenant != null) {
                this.pointDeVenteId = tenant;
            } else {
                this.pointDeVenteId = 1L;
            }
        }
    }

    // Constructors, getters, setters
    public Commande() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public StatutLivraison getStatutLivraison() { return statutLivraison; }
    public void setStatutLivraison(StatutLivraison statutLivraison) { this.statutLivraison = statutLivraison; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public LocalDateTime getDateLivraisonReelle() { return dateLivraisonReelle; }
    public void setDateLivraisonReelle(LocalDateTime dateLivraisonReelle) { this.dateLivraisonReelle = dateLivraisonReelle; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public Boolean getAppliquerTVA() { return appliquerTVA; }
    public void setAppliquerTVA(Boolean appliquerTVA) { this.appliquerTVA = appliquerTVA; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<LigneCommande> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommande> lignesCommande) { this.lignesCommande = lignesCommande; }

    public List<Livraison> getLivraisons() { return livraisons; }
    public void setLivraisons(List<Livraison> livraisons) { this.livraisons = livraisons; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getDevise() { return devise != null ? devise : "MAD"; }
    public void setDevise(String devise) { this.devise = devise; }

    public com.gestion.persistent.enums.TypeAchat getTypeAchat() { return typeAchat; }
    public void setTypeAchat(com.gestion.persistent.enums.TypeAchat typeAchat) { this.typeAchat = typeAchat; }

    public boolean isInternational() { return typeAchat == com.gestion.persistent.enums.TypeAchat.INTERNATIONAL; }

    public com.gestion.persistent.enums.Incoterm getIncoterm() { return incoterm; }
    public void setIncoterm(com.gestion.persistent.enums.Incoterm incoterm) { this.incoterm = incoterm; }

    public String getLieuIncoterm() { return lieuIncoterm; }
    public void setLieuIncoterm(String lieuIncoterm) { this.lieuIncoterm = lieuIncoterm; }

    public String getPortEmbarquement() { return portEmbarquement; }
    public void setPortEmbarquement(String portEmbarquement) { this.portEmbarquement = portEmbarquement; }

    public LieuLogistique getLieuDestination() { return lieuDestination; }
    public void setLieuDestination(LieuLogistique lieuDestination) { this.lieuDestination = lieuDestination; }

    public com.gestion.persistent.enums.ModeTransport getModeTransport() { return modeTransport; }
    public void setModeTransport(com.gestion.persistent.enums.ModeTransport modeTransport) { this.modeTransport = modeTransport; }

    public ConditionPaiement getConditionPaiement() { return conditionPaiement; }
    public void setConditionPaiement(ConditionPaiement conditionPaiement) { this.conditionPaiement = conditionPaiement; }

    public BigDecimal getTauxChangeCommande() { return tauxChangeCommande; }
    public void setTauxChangeCommande(BigDecimal tauxChangeCommande) { this.tauxChangeCommande = tauxChangeCommande; }

    public BigDecimal getTauxChangeDouane() { return tauxChangeDouane; }
    public void setTauxChangeDouane(BigDecimal tauxChangeDouane) { this.tauxChangeDouane = tauxChangeDouane; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public String getNumeroConteneur() { return numeroConteneur; }
    public void setNumeroConteneur(String numeroConteneur) { this.numeroConteneur = numeroConteneur; }

    public String getNavire() { return navire; }
    public void setNavire(String navire) { this.navire = navire; }

    public LocalDateTime getDateEtd() { return dateEtd; }
    public void setDateEtd(LocalDateTime dateEtd) { this.dateEtd = dateEtd; }

    public LocalDateTime getDateEta() { return dateEta; }
    public void setDateEta(LocalDateTime dateEta) { this.dateEta = dateEta; }

    public String getNumeroDum() { return numeroDum; }
    public void setNumeroDum(String numeroDum) { this.numeroDum = numeroDum; }

    public LocalDateTime getDateDum() { return dateDum; }
    public void setDateDum(LocalDateTime dateDum) { this.dateDum = dateDum; }

    // PI
    public String getNumeroPi() { return numeroPi; }
    public void setNumeroPi(String numeroPi) { this.numeroPi = numeroPi; }

    public java.time.LocalDate getDatePi() { return datePi; }
    public void setDatePi(java.time.LocalDate datePi) { this.datePi = datePi; }

    public String getReferencePi() { return referencePi; }
    public void setReferencePi(String referencePi) { this.referencePi = referencePi; }

    // PortNet & Domiciliation
    public String getNumeroEngagementPortnet() { return numeroEngagementPortnet; }
    public void setNumeroEngagementPortnet(String n) { this.numeroEngagementPortnet = n; }

    public java.time.LocalDate getDateEngagementPortnet() { return dateEngagementPortnet; }
    public void setDateEngagementPortnet(java.time.LocalDate d) { this.dateEngagementPortnet = d; }

    public String getBanqueDomiciliation() { return banqueDomiciliation; }
    public void setBanqueDomiciliation(String banqueDomiciliation) { this.banqueDomiciliation = banqueDomiciliation; }

    public String getAgenceDomiciliation() { return agenceDomiciliation; }
    public void setAgenceDomiciliation(String agenceDomiciliation) { this.agenceDomiciliation = agenceDomiciliation; }

    public String getStatutWorkflowImport() { return statutWorkflowImport; }
    public void setStatutWorkflowImport(String statutWorkflowImport) { this.statutWorkflowImport = statutWorkflowImport; }

    public java.time.LocalDate getDateEnvoiBanque() { return dateEnvoiBanque; }
    public void setDateEnvoiBanque(java.time.LocalDate dateEnvoiBanque) { this.dateEnvoiBanque = dateEnvoiBanque; }

    public String getNumeroOrdreTransfert() { return numeroOrdreTransfert; }
    public void setNumeroOrdreTransfert(String numeroOrdreTransfert) { this.numeroOrdreTransfert = numeroOrdreTransfert; }

    public BigDecimal getMontantMarchandiseDevise() { return montantMarchandiseDevise; }
    public void setMontantMarchandiseDevise(BigDecimal montantMarchandiseDevise) { this.montantMarchandiseDevise = montantMarchandiseDevise; }

    public BigDecimal getMontantMarchandiseMad() { return montantMarchandiseMad; }
    public void setMontantMarchandiseMad(BigDecimal montantMarchandiseMad) { this.montantMarchandiseMad = montantMarchandiseMad; }

    public BigDecimal getValeurDouaneMad() { return valeurDouaneMad; }
    public void setValeurDouaneMad(BigDecimal valeurDouaneMad) { this.valeurDouaneMad = valeurDouaneMad; }

    public BigDecimal getTotalChargesEstimeesMad() { return totalChargesEstimeesMad; }
    public void setTotalChargesEstimeesMad(BigDecimal totalChargesEstimeesMad) { this.totalChargesEstimeesMad = totalChargesEstimeesMad; }

    public BigDecimal getTotalChargesReellesMad() { return totalChargesReellesMad; }
    public void setTotalChargesReellesMad(BigDecimal totalChargesReellesMad) { this.totalChargesReellesMad = totalChargesReellesMad; }

    public BigDecimal getTotalChargesMad() { return totalChargesMad; }
    public void setTotalChargesMad(BigDecimal totalChargesMad) { this.totalChargesMad = totalChargesMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }

    public BigDecimal getCoefficientApproche() { return coefficientApproche; }
    public void setCoefficientApproche(BigDecimal coefficientApproche) { this.coefficientApproche = coefficientApproche; }

    public com.gestion.persistent.enums.StatutCouts getStatutCouts() { return statutCouts; }
    public void setStatutCouts(com.gestion.persistent.enums.StatutCouts statutCouts) { this.statutCouts = statutCouts; }
}
