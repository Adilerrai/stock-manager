package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gestion.persistent.enums.CleRepartition;
import com.gestion.persistent.enums.StatutCharge;
import com.gestion.persistent.enums.TypeCharge;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ChargeAchatDTO {

    private Long id;
    private Long commandeId;
    private Long ligneCommandeId;
    private String ligneCommandeDesignation;
    private TypeCharge typeCharge;
    private String typeChargeLibelle;
    private String libelle;
    private Long prestataireId;
    private String prestataireNom;
    private Long factureAchatId;
    private String numeroFactureAchat;
    private Long depenseId;
    private String referenceDepense;
    private String numeroFactureExterne;
    private String devise = "MAD";
    private BigDecimal tauxChange = BigDecimal.ONE;
    private BigDecimal montantEstimeDevise = BigDecimal.ZERO;
    private BigDecimal montantReelDevise;
    private BigDecimal montantRetenuMad = BigDecimal.ZERO;
    private BigDecimal montantTvaMad = BigDecimal.ZERO;
    private CleRepartition cleRepartition = CleRepartition.VALEUR;
    private Boolean incorporable = true;
    private StatutCharge statut = StatutCharge.ESTIMEE;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateCharge;
    private Boolean appliqueeAuStock = false;
    private String observations;

    public ChargeAchatDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getLigneCommandeId() {
        return ligneCommandeId;
    }

    public void setLigneCommandeId(Long ligneCommandeId) {
        this.ligneCommandeId = ligneCommandeId;
    }

    public String getLigneCommandeDesignation() {
        return ligneCommandeDesignation;
    }

    public void setLigneCommandeDesignation(String ligneCommandeDesignation) {
        this.ligneCommandeDesignation = ligneCommandeDesignation;
    }

    public TypeCharge getTypeCharge() {
        return typeCharge;
    }

    public void setTypeCharge(TypeCharge typeCharge) {
        this.typeCharge = typeCharge;
        if (typeCharge != null && this.typeChargeLibelle == null) {
            this.typeChargeLibelle = typeCharge.getLibelle();
        }
    }

    public String getTypeChargeLibelle() {
        return typeChargeLibelle;
    }

    public void setTypeChargeLibelle(String typeChargeLibelle) {
        this.typeChargeLibelle = typeChargeLibelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getPrestataireId() {
        return prestataireId;
    }

    public void setPrestataireId(Long prestataireId) {
        this.prestataireId = prestataireId;
    }

    public String getPrestataireNom() {
        return prestataireNom;
    }

    public void setPrestataireNom(String prestataireNom) {
        this.prestataireNom = prestataireNom;
    }

    public Long getFactureAchatId() {
        return factureAchatId;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }

    public String getNumeroFactureAchat() {
        return numeroFactureAchat;
    }

    public void setNumeroFactureAchat(String numeroFactureAchat) {
        this.numeroFactureAchat = numeroFactureAchat;
    }

    public Long getDepenseId() {
        return depenseId;
    }

    public void setDepenseId(Long depenseId) {
        this.depenseId = depenseId;
    }

    public String getReferenceDepense() {
        return referenceDepense;
    }

    public void setReferenceDepense(String referenceDepense) {
        this.referenceDepense = referenceDepense;
    }

    public String getNumeroFactureExterne() {
        return numeroFactureExterne;
    }

    public void setNumeroFactureExterne(String numeroFactureExterne) {
        this.numeroFactureExterne = numeroFactureExterne;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public BigDecimal getTauxChange() {
        return tauxChange;
    }

    public void setTauxChange(BigDecimal tauxChange) {
        this.tauxChange = tauxChange;
    }

    public BigDecimal getMontantEstimeDevise() {
        return montantEstimeDevise;
    }

    public void setMontantEstimeDevise(BigDecimal montantEstimeDevise) {
        this.montantEstimeDevise = montantEstimeDevise;
    }

    public BigDecimal getMontantReelDevise() {
        return montantReelDevise;
    }

    public void setMontantReelDevise(BigDecimal montantReelDevise) {
        this.montantReelDevise = montantReelDevise;
    }

    public BigDecimal getMontantRetenuMad() {
        return montantRetenuMad;
    }

    public void setMontantRetenuMad(BigDecimal montantRetenuMad) {
        this.montantRetenuMad = montantRetenuMad;
    }

    public BigDecimal getMontantTvaMad() {
        return montantTvaMad;
    }

    public void setMontantTvaMad(BigDecimal montantTvaMad) {
        this.montantTvaMad = montantTvaMad;
    }

    public CleRepartition getCleRepartition() {
        return cleRepartition;
    }

    public void setCleRepartition(CleRepartition cleRepartition) {
        this.cleRepartition = cleRepartition;
    }

    public Boolean getIncorporable() {
        return incorporable;
    }

    public void setIncorporable(Boolean incorporable) {
        this.incorporable = incorporable;
    }

    public StatutCharge getStatut() {
        return statut;
    }

    public void setStatut(StatutCharge statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCharge() {
        return dateCharge;
    }

    public void setDateCharge(LocalDateTime dateCharge) {
        this.dateCharge = dateCharge;
    }

    public Boolean getAppliqueeAuStock() {
        return appliqueeAuStock;
    }

    public void setAppliqueeAuStock(Boolean appliqueeAuStock) {
        this.appliqueeAuStock = appliqueeAuStock;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
