package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.enums.ModePaiement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "depenses")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String designation;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "date_depense", nullable = false)
    private LocalDate dateDepense = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategorieDepense categorie;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement = ModePaiement.ESPECES;

    private String beneficiaire;

    @Column(name = "numero_facture_justificatif")
    private String numeroFactureJustificatif;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @Column(name = "est_frais_import")
    private Boolean estFraisImport = false;

    @Column(name = "type_frais_import", length = 50)
    private String typeFraisImport;

    // ===== PortNet / Douane =====

    /** N° Engagement PortNet associé à cette dépense (ex: frais transitaire, magasinage) */
    @Column(name = "numero_engagement_portnet", length = 100)
    private String numeroEngagementPortnet;

    /** Type de document PortNet : ENGAGEMENT, BON_A_DEDOMICILIER, BON_LIVRAISON_DOUANE, etc. */
    @Column(name = "type_document_portnet", length = 50)
    private String typeDocumentPortnet;

    /** Référence du dossier PortNet / ADII (ex: numéro DUM, code titre) */
    @Column(name = "reference_portnet", length = 100)
    private String referencePortnet;

    // ===== Devise (pour dépenses en devises : fret, assurance...) =====

    @Column(name = "montant_devise", precision = 15, scale = 2)
    private BigDecimal montantDevise;

    @Column(name = "devise_depense", length = 10)
    private String deviseDepense = "MAD";

    @Column(name = "taux_change_depense", precision = 12, scale = 6)
    private BigDecimal tauxChangeDepense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestataire_id")
    private Prestataire prestataire;

    @OneToMany(mappedBy = "depense", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<LigneDepenseConsommable> lignesConsommables = new java.util.ArrayList<>();

    @Column(name = "articles_detail", columnDefinition = "TEXT")
    private String articlesDetail;

    @Column(name = "deversee_compta")
    private Boolean deverseeCompta = false;

    @Column(name = "date_deversement")
    private LocalDateTime dateDeversement;

    @Column(name = "ecriture_id")
    private Long ecritureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Depense() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateDepense() { return dateDepense; }
    public void setDateDepense(LocalDate dateDepense) { this.dateDepense = dateDepense; }

    public CategorieDepense getCategorie() { return categorie; }
    public void setCategorie(CategorieDepense categorie) { this.categorie = categorie; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public String getNumeroFactureJustificatif() { return numeroFactureJustificatif; }
    public void setNumeroFactureJustificatif(String numeroFactureJustificatif) { this.numeroFactureJustificatif = numeroFactureJustificatif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreePar() { return creePar; }
    public void setCreePar(User creePar) { this.creePar = creePar; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; }

    public String getArticlesDetail() { return articlesDetail; }
    public void setArticlesDetail(String articlesDetail) { this.articlesDetail = articlesDetail; }

    public Boolean getDeverseeCompta() { return deverseeCompta; }
    public void setDeverseeCompta(Boolean deverseeCompta) { this.deverseeCompta = deverseeCompta; }

    public LocalDateTime getDateDeversement() { return dateDeversement; }
    public void setDateDeversement(LocalDateTime dateDeversement) { this.dateDeversement = dateDeversement; }

    public Long getEcritureId() { return ecritureId; }
    public void setEcritureId(Long ecritureId) { this.ecritureId = ecritureId; }

    public Prestataire getPrestataire() { return prestataire; }
    public void setPrestataire(Prestataire prestataire) { this.prestataire = prestataire; }

    public java.util.List<LigneDepenseConsommable> getLignesConsommables() { return lignesConsommables; }
    public void setLignesConsommables(java.util.List<LigneDepenseConsommable> lignesConsommables) { this.lignesConsommables = lignesConsommables; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public Boolean getEstFraisImport() { return estFraisImport; }
    public void setEstFraisImport(Boolean estFraisImport) { this.estFraisImport = estFraisImport; }

    public String getTypeFraisImport() { return typeFraisImport; }
    public void setTypeFraisImport(String typeFraisImport) { this.typeFraisImport = typeFraisImport; }

    // PortNet
    public String getNumeroEngagementPortnet() { return numeroEngagementPortnet; }
    public void setNumeroEngagementPortnet(String numeroEngagementPortnet) { this.numeroEngagementPortnet = numeroEngagementPortnet; }

    public String getTypeDocumentPortnet() { return typeDocumentPortnet; }
    public void setTypeDocumentPortnet(String typeDocumentPortnet) { this.typeDocumentPortnet = typeDocumentPortnet; }

    public String getReferencePortnet() { return referencePortnet; }
    public void setReferencePortnet(String referencePortnet) { this.referencePortnet = referencePortnet; }

    // Devise
    public BigDecimal getMontantDevise() { return montantDevise; }
    public void setMontantDevise(BigDecimal montantDevise) { this.montantDevise = montantDevise; }

    public String getDeviseDepense() { return deviseDepense; }
    public void setDeviseDepense(String deviseDepense) { this.deviseDepense = deviseDepense; }

    public BigDecimal getTauxChangeDepense() { return tauxChangeDepense; }
    public void setTauxChangeDepense(BigDecimal tauxChangeDepense) { this.tauxChangeDepense = tauxChangeDepense; }
}
