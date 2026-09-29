package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeOperationRas;
import java.math.BigDecimal;

/**
 * Requête de simulation / calcul de Retenue à la Source (RAS TVA & Honoraires).
 */
public class RasCalculRequest {

    private BigDecimal montantHt;
    private BigDecimal tauxTva = new BigDecimal("20.0");
    private TypeOperationRas typeOperation = TypeOperationRas.PRESTATIONS_SERVICES;
    private Boolean attestationRegularitePresente = false;
    private Long fournisseurId;
    private Long factureAchatId;

    public RasCalculRequest() {}

    public RasCalculRequest(BigDecimal montantHt, BigDecimal tauxTva, TypeOperationRas typeOperation, Boolean attestationRegularitePresente) {
        this.montantHt = montantHt;
        this.tauxTva = tauxTva != null ? tauxTva : new BigDecimal("20.0");
        this.typeOperation = typeOperation != null ? typeOperation : TypeOperationRas.PRESTATIONS_SERVICES;
        this.attestationRegularitePresente = attestationRegularitePresente != null ? attestationRegularitePresente : false;
    }

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

    public TypeOperationRas getTypeOperation() {
        return typeOperation;
    }

    public void setTypeOperation(TypeOperationRas typeOperation) {
        this.typeOperation = typeOperation;
    }

    public Boolean getAttestationRegularitePresente() {
        return attestationRegularitePresente != null && attestationRegularitePresente;
    }

    public void setAttestationRegularitePresente(Boolean attestationRegularitePresente) {
        this.attestationRegularitePresente = attestationRegularitePresente;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public Long getFactureAchatId() {
        return factureAchatId;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }
}
