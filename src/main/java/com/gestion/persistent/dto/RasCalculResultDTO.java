package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeOperationRas;
import java.math.BigDecimal;

/**
 * Résultat détaillé du calcul automatique de la Retenue à la Source (CGI Maroc).
 */
public class RasCalculResultDTO {

    private BigDecimal montantHt;
    private BigDecimal tauxTva;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
    private TypeOperationRas typeOperation;
    private Boolean attestationRegularitePresente;
    private BigDecimal tauxRas; // en % (ex: 75.0 ou 100.0)
    private BigDecimal montantRas; // Montant retenu
    private BigDecimal montantNetAPayer; // Montant TTC - RAS
    private String baseCalcul; // "TVA" ou "HT"
    private String justificationLegale;
    private String compteTvaRetenue = "44580000";
    private String compteFournisseur = "44110000";
    private String compteTresorerie = "51410000";

    public RasCalculResultDTO() {}

    public BigDecimal getMontantHt() {
        return montantHt;
    }

    public void setMontantHt(BigDecimal montantHt) {
        this.montantHt = montantHt;
    }

    public BigDecimal getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(BigDecimal tauxTva) {
        this.tauxTva = tauxTva;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public void setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
    }

    public BigDecimal getMontantTtc() {
        return montantTtc;
    }

    public void setMontantTtc(BigDecimal montantTtc) {
        this.montantTtc = montantTtc;
    }

    public TypeOperationRas getTypeOperation() {
        return typeOperation;
    }

    public void setTypeOperation(TypeOperationRas typeOperation) {
        this.typeOperation = typeOperation;
    }

    public Boolean getAttestationRegularitePresente() {
        return attestationRegularitePresente;
    }

    public void setAttestationRegularitePresente(Boolean attestationRegularitePresente) {
        this.attestationRegularitePresente = attestationRegularitePresente;
    }

    public BigDecimal getTauxRas() {
        return tauxRas;
    }

    public void setTauxRas(BigDecimal tauxRas) {
        this.tauxRas = tauxRas;
    }

    public BigDecimal getMontantRas() {
        return montantRas;
    }

    public void setMontantRas(BigDecimal montantRas) {
        this.montantRas = montantRas;
    }

    public BigDecimal getMontantNetAPayer() {
        return montantNetAPayer;
    }

    public void setMontantNetAPayer(BigDecimal montantNetAPayer) {
        this.montantNetAPayer = montantNetAPayer;
    }

    public String getBaseCalcul() {
        return baseCalcul;
    }

    public void setBaseCalcul(String baseCalcul) {
        this.baseCalcul = baseCalcul;
    }

    public String getJustificationLegale() {
        return justificationLegale;
    }

    public void setJustificationLegale(String justificationLegale) {
        this.justificationLegale = justificationLegale;
    }

    public String getCompteTvaRetenue() {
        return compteTvaRetenue;
    }

    public void setCompteTvaRetenue(String compteTvaRetenue) {
        this.compteTvaRetenue = compteTvaRetenue;
    }

    public String getCompteFournisseur() {
        return compteFournisseur;
    }

    public void setCompteFournisseur(String compteFournisseur) {
        this.compteFournisseur = compteFournisseur;
    }

    public String getCompteTresorerie() {
        return compteTresorerie;
    }

    public void setCompteTresorerie(String compteTresorerie) {
        this.compteTresorerie = compteTresorerie;
    }
}
