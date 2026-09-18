package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gestion.persistent.enums.StatutImmobilisation;
import com.gestion.persistent.enums.TypeAmortissement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "immobilisations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"code", "point_de_vente_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Immobilisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 255)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_immobilisation_id", nullable = false)
    private CompteComptable compteImmobilisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_amortissement_id", nullable = false)
    private CompteComptable compteAmortissement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_dotation_id", nullable = false)
    private CompteComptable compteDotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_produit_cession_id")
    private CompteComptable compteProduitCession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_vna_id")
    private CompteComptable compteVna;

    @Column(name = "numero_facture", length = 100)
    private String numeroFacture;

    @Column(name = "fournisseur_nom", length = 150)
    private String fournisseurNom;

    @Column(name = "date_acquisition", nullable = false)
    private LocalDate dateAcquisition;

    @Column(name = "date_mise_en_service", nullable = false)
    private LocalDate dateMiseEnService;

    @Column(name = "valeur_acquisition", precision = 15, scale = 2, nullable = false)
    private BigDecimal valeurAcquisition = BigDecimal.ZERO;

    @Column(name = "tva_deductible", precision = 15, scale = 2)
    private BigDecimal tvaDeductible = BigDecimal.ZERO;

    @Column(name = "valeur_residuelle", precision = 15, scale = 2)
    private BigDecimal valeurResiduelle = BigDecimal.ZERO;

    @Column(name = "duree_annees", nullable = false)
    private Integer dureeAnnees = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_amortissement", nullable = false, length = 30)
    private TypeAmortissement typeAmortissement = TypeAmortissement.LINEAIRE;

    @Column(name = "taux_amortissement", precision = 8, scale = 4)
    private BigDecimal tauxAmortissement = BigDecimal.ZERO;

    @Column(name = "coefficient_degressif", precision = 4, scale = 2)
    private BigDecimal coefficientDegressif = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutImmobilisation statut = StatutImmobilisation.EN_SERVICE;

    @Column(name = "date_cession")
    private LocalDate dateCession;

    @Column(name = "prix_cession", precision = 15, scale = 2)
    private BigDecimal prixCession = BigDecimal.ZERO;

    @Column(name = "cumul_amortissements", precision = 15, scale = 2)
    private BigDecimal cumulAmortissements = BigDecimal.ZERO;

    @Column(name = "valeur_nette_comptable", precision = 15, scale = 2)
    private BigDecimal valeurNetteComptable = BigDecimal.ZERO;

    @OneToMany(mappedBy = "immobilisation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("annee ASC")
    @JsonManagedReference
    private List<LignePlanAmortissement> lignesPlanAmortissement = new ArrayList<>();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Immobilisation() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public CompteComptable getCompteImmobilisation() { return compteImmobilisation; }
    public void setCompteImmobilisation(CompteComptable compteImmobilisation) { this.compteImmobilisation = compteImmobilisation; }

    public CompteComptable getCompteAmortissement() { return compteAmortissement; }
    public void setCompteAmortissement(CompteComptable compteAmortissement) { this.compteAmortissement = compteAmortissement; }

    public CompteComptable getCompteDotation() { return compteDotation; }
    public void setCompteDotation(CompteComptable compteDotation) { this.compteDotation = compteDotation; }

    public CompteComptable getCompteProduitCession() { return compteProduitCession; }
    public void setCompteProduitCession(CompteComptable compteProduitCession) { this.compteProduitCession = compteProduitCession; }

    public CompteComptable getCompteVna() { return compteVna; }
    public void setCompteVna(CompteComptable compteVna) { this.compteVna = compteVna; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public LocalDate getDateAcquisition() { return dateAcquisition; }
    public void setDateAcquisition(LocalDate dateAcquisition) { this.dateAcquisition = dateAcquisition; }

    public LocalDate getDateMiseEnService() { return dateMiseEnService; }
    public void setDateMiseEnService(LocalDate dateMiseEnService) { this.dateMiseEnService = dateMiseEnService; }

    public BigDecimal getValeurAcquisition() { return valeurAcquisition; }
    public void setValeurAcquisition(BigDecimal valeurAcquisition) { this.valeurAcquisition = valeurAcquisition; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible; }

    public BigDecimal getValeurResiduelle() { return valeurResiduelle; }
    public void setValeurResiduelle(BigDecimal valeurResiduelle) { this.valeurResiduelle = valeurResiduelle; }

    public Integer getDureeAnnees() { return dureeAnnees; }
    public void setDureeAnnees(Integer dureeAnnees) { this.dureeAnnees = dureeAnnees; }

    public TypeAmortissement getTypeAmortissement() { return typeAmortissement; }
    public void setTypeAmortissement(TypeAmortissement typeAmortissement) { this.typeAmortissement = typeAmortissement; }

    public BigDecimal getTauxAmortissement() { return tauxAmortissement; }
    public void setTauxAmortissement(BigDecimal tauxAmortissement) { this.tauxAmortissement = tauxAmortissement; }

    public BigDecimal getCoefficientDegressif() { return coefficientDegressif; }
    public void setCoefficientDegressif(BigDecimal coefficientDegressif) { this.coefficientDegressif = coefficientDegressif; }

    public StatutImmobilisation getStatut() { return statut; }
    public void setStatut(StatutImmobilisation statut) { this.statut = statut; }

    public LocalDate getDateCession() { return dateCession; }
    public void setDateCession(LocalDate dateCession) { this.dateCession = dateCession; }

    public BigDecimal getPrixCession() { return prixCession; }
    public void setPrixCession(BigDecimal prixCession) { this.prixCession = prixCession; }

    public BigDecimal getCumulAmortissements() { return cumulAmortissements; }
    public void setCumulAmortissements(BigDecimal cumulAmortissements) { this.cumulAmortissements = cumulAmortissements; }

    public BigDecimal getValeurNetteComptable() { return valeurNetteComptable; }
    public void setValeurNetteComptable(BigDecimal valeurNetteComptable) { this.valeurNetteComptable = valeurNetteComptable; }

    public List<LignePlanAmortissement> getLignesPlanAmortissement() { return lignesPlanAmortissement; }
    public void setLignesPlanAmortissement(List<LignePlanAmortissement> lignesPlanAmortissement) { this.lignesPlanAmortissement = lignesPlanAmortissement; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
