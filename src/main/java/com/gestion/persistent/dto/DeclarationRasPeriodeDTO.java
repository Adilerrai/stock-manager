package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Bordereau récapitulatif périodique de Retenue à la Source (TVA et autres).
 * Permet d'alimenter directement la Ligne 138 de la déclaration SIMPL-TVA.
 */
public class DeclarationRasPeriodeDTO {

    private Integer annee;
    private Integer mois;
    private Integer trimestre;
    private String societeRaisonSociale;
    private String societeIce;
    private String societeIf;

    private int nombreRetenues;
    private BigDecimal totalHt = BigDecimal.ZERO;
    private BigDecimal totalTva = BigDecimal.ZERO;
    private BigDecimal totalRas = BigDecimal.ZERO;
    private BigDecimal totalNetPaye = BigDecimal.ZERO;

    /**
     * Montant exact à reporter sur la déclaration SIMPL-TVA (Ligne 138 - TVA retenue à la source à reverser).
     */
    private BigDecimal ligne138SimplTva = BigDecimal.ZERO;

    private List<AttestationRasDTO> attestations = new ArrayList<>();

    public DeclarationRasPeriodeDTO() {}

    public Integer getAnnee() {
        return annee;
    }

    public void setAnnee(Integer annee) {
        this.annee = annee;
    }

    public Integer getMois() {
        return mois;
    }

    public void setMois(Integer mois) {
        this.mois = mois;
    }

    public Integer getTrimestre() {
        return trimestre;
    }

    public void setTrimestre(Integer trimestre) {
        this.trimestre = trimestre;
    }

    public String getSocieteRaisonSociale() {
        return societeRaisonSociale;
    }

    public void setSocieteRaisonSociale(String societeRaisonSociale) {
        this.societeRaisonSociale = societeRaisonSociale;
    }

    public String getSocieteIce() {
        return societeIce;
    }

    public void setSocieteIce(String societeIce) {
        this.societeIce = societeIce;
    }

    public String getSocieteIf() {
        return societeIf;
    }

    public void setSocieteIf(String societeIf) {
        this.societeIf = societeIf;
    }

    public int getNombreRetenues() {
        return nombreRetenues;
    }

    public void setNombreRetenues(int nombreRetenues) {
        this.nombreRetenues = nombreRetenues;
    }

    public BigDecimal getTotalHt() {
        return totalHt;
    }

    public void setTotalHt(BigDecimal totalHt) {
        this.totalHt = totalHt;
    }

    public BigDecimal getTotalTva() {
        return totalTva;
    }

    public void setTotalTva(BigDecimal totalTva) {
        this.totalTva = totalTva;
    }

    public BigDecimal getTotalRas() {
        return totalRas;
    }

    public void setTotalRas(BigDecimal totalRas) {
        this.totalRas = totalRas;
    }

    public BigDecimal getTotalNetPaye() {
        return totalNetPaye;
    }

    public void setTotalNetPaye(BigDecimal totalNetPaye) {
        this.totalNetPaye = totalNetPaye;
    }

    public BigDecimal getLigne138SimplTva() {
        return ligne138SimplTva;
    }

    public void setLigne138SimplTva(BigDecimal ligne138SimplTva) {
        this.ligne138SimplTva = ligne138SimplTva;
    }

    public List<AttestationRasDTO> getAttestations() {
        return attestations;
    }

    public void setAttestations(List<AttestationRasDTO> attestations) {
        this.attestations = attestations;
    }
}
