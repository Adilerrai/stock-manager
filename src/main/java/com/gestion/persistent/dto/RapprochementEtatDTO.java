package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RapprochementEtatDTO {
    private Long compteId;
    private String compteNom;
    private String numeroRib;
    private String banqueNom;
    private LocalDate dateArrete;

    private BigDecimal soldeReleve = BigDecimal.ZERO;
    private BigDecimal soldeComptable = BigDecimal.ZERO;
    private BigDecimal ecart = BigDecimal.ZERO;

    private BigDecimal totalDebitsReleveEnSuspens = BigDecimal.ZERO;
    private BigDecimal totalCreditsReleveEnSuspens = BigDecimal.ZERO;

    private List<LigneReleveBancaireDTO> lignesReleveEnAttente = new ArrayList<>();
    private List<MouvementTresorerieDTO> mouvementsEnAttente = new ArrayList<>();

    public RapprochementEtatDTO() {}

    public Long getCompteId() {
        return compteId;
    }

    public void setCompteId(Long compteId) {
        this.compteId = compteId;
    }

    public String getCompteNom() {
        return compteNom;
    }

    public void setCompteNom(String compteNom) {
        this.compteNom = compteNom;
    }

    public String getNumeroRib() {
        return numeroRib;
    }

    public void setNumeroRib(String numeroRib) {
        this.numeroRib = numeroRib;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
    }

    public LocalDate getDateArrete() {
        return dateArrete;
    }

    public void setDateArrete(LocalDate dateArrete) {
        this.dateArrete = dateArrete;
    }

    public BigDecimal getSoldeReleve() {
        return soldeReleve;
    }

    public void setSoldeReleve(BigDecimal soldeReleve) {
        this.soldeReleve = soldeReleve;
    }

    public BigDecimal getSoldeComptable() {
        return soldeComptable;
    }

    public void setSoldeComptable(BigDecimal soldeComptable) {
        this.soldeComptable = soldeComptable;
    }

    public BigDecimal getEcart() {
        return ecart;
    }

    public void setEcart(BigDecimal ecart) {
        this.ecart = ecart;
    }

    public BigDecimal getTotalDebitsReleveEnSuspens() {
        return totalDebitsReleveEnSuspens;
    }

    public void setTotalDebitsReleveEnSuspens(BigDecimal totalDebitsReleveEnSuspens) {
        this.totalDebitsReleveEnSuspens = totalDebitsReleveEnSuspens;
    }

    public BigDecimal getTotalCreditsReleveEnSuspens() {
        return totalCreditsReleveEnSuspens;
    }

    public void setTotalCreditsReleveEnSuspens(BigDecimal totalCreditsReleveEnSuspens) {
        this.totalCreditsReleveEnSuspens = totalCreditsReleveEnSuspens;
    }

    public List<LigneReleveBancaireDTO> getLignesReleveEnAttente() {
        return lignesReleveEnAttente;
    }

    public void setLignesReleveEnAttente(List<LigneReleveBancaireDTO> lignesReleveEnAttente) {
        this.lignesReleveEnAttente = lignesReleveEnAttente;
    }

    public List<MouvementTresorerieDTO> getMouvementsEnAttente() {
        return mouvementsEnAttente;
    }

    public void setMouvementsEnAttente(List<MouvementTresorerieDTO> mouvementsEnAttente) {
        this.mouvementsEnAttente = mouvementsEnAttente;
    }
}
