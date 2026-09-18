package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutImmobilisation;
import com.gestion.persistent.enums.TypeAmortissement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ImmobilisationDTO {

    private Long id;
    private String code;
    private String designation;

    private Long compteImmobilisationId;
    private String compteImmobilisationNumero;
    private String compteImmobilisationLibelle;

    private Long compteAmortissementId;
    private String compteAmortissementNumero;
    private String compteAmortissementLibelle;

    private Long compteDotationId;
    private String compteDotationNumero;
    private String compteDotationLibelle;

    private Long compteProduitCessionId;
    private String compteProduitCessionNumero;

    private Long compteVnaId;
    private String compteVnaNumero;

    private String numeroFacture;
    private String fournisseurNom;

    private LocalDate dateAcquisition;
    private LocalDate dateMiseEnService;

    private BigDecimal valeurAcquisition = BigDecimal.ZERO;
    private BigDecimal tvaDeductible = BigDecimal.ZERO;
    private BigDecimal valeurResiduelle = BigDecimal.ZERO;

    private Integer dureeAnnees = 5;
    private TypeAmortissement typeAmortissement = TypeAmortissement.LINEAIRE;
    private BigDecimal tauxAmortissement = BigDecimal.ZERO;
    private BigDecimal coefficientDegressif = BigDecimal.ONE;

    private StatutImmobilisation statut = StatutImmobilisation.EN_SERVICE;
    private LocalDate dateCession;
    private BigDecimal prixCession = BigDecimal.ZERO;

    private BigDecimal cumulAmortissements = BigDecimal.ZERO;
    private BigDecimal valeurNetteComptable = BigDecimal.ZERO;

    private List<LignePlanAmortissementDTO> lignesPlanAmortissement = new ArrayList<>();

    private Long pointDeVenteId;
    private LocalDateTime dateCreation;

    public ImmobilisationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public Long getCompteImmobilisationId() { return compteImmobilisationId; }
    public void setCompteImmobilisationId(Long compteImmobilisationId) { this.compteImmobilisationId = compteImmobilisationId; }

    public String getCompteImmobilisationNumero() { return compteImmobilisationNumero; }
    public void setCompteImmobilisationNumero(String compteImmobilisationNumero) { this.compteImmobilisationNumero = compteImmobilisationNumero; }

    public String getCompteImmobilisationLibelle() { return compteImmobilisationLibelle; }
    public void setCompteImmobilisationLibelle(String compteImmobilisationLibelle) { this.compteImmobilisationLibelle = compteImmobilisationLibelle; }

    public Long getCompteAmortissementId() { return compteAmortissementId; }
    public void setCompteAmortissementId(Long compteAmortissementId) { this.compteAmortissementId = compteAmortissementId; }

    public String getCompteAmortissementNumero() { return compteAmortissementNumero; }
    public void setCompteAmortissementNumero(String compteAmortissementNumero) { this.compteAmortissementNumero = compteAmortissementNumero; }

    public String getCompteAmortissementLibelle() { return compteAmortissementLibelle; }
    public void setCompteAmortissementLibelle(String compteAmortissementLibelle) { this.compteAmortissementLibelle = compteAmortissementLibelle; }

    public Long getCompteDotationId() { return compteDotationId; }
    public void setCompteDotationId(Long compteDotationId) { this.compteDotationId = compteDotationId; }

    public String getCompteDotationNumero() { return compteDotationNumero; }
    public void setCompteDotationNumero(String compteDotationNumero) { this.compteDotationNumero = compteDotationNumero; }

    public String getCompteDotationLibelle() { return compteDotationLibelle; }
    public void setCompteDotationLibelle(String compteDotationLibelle) { this.compteDotationLibelle = compteDotationLibelle; }

    public Long getCompteProduitCessionId() { return compteProduitCessionId; }
    public void setCompteProduitCessionId(Long compteProduitCessionId) { this.compteProduitCessionId = compteProduitCessionId; }

    public String getCompteProduitCessionNumero() { return compteProduitCessionNumero; }
    public void setCompteProduitCessionNumero(String compteProduitCessionNumero) { this.compteProduitCessionNumero = compteProduitCessionNumero; }

    public Long getCompteVnaId() { return compteVnaId; }
    public void setCompteVnaId(Long compteVnaId) { this.compteVnaId = compteVnaId; }

    public String getCompteVnaNumero() { return compteVnaNumero; }
    public void setCompteVnaNumero(String compteVnaNumero) { this.compteVnaNumero = compteVnaNumero; }

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

    public List<LignePlanAmortissementDTO> getLignesPlanAmortissement() { return lignesPlanAmortissement; }
    public void setLignesPlanAmortissement(List<LignePlanAmortissementDTO> lignesPlanAmortissement) { this.lignesPlanAmortissement = lignesPlanAmortissement; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
