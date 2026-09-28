package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RapprochementComparatif5141DTO {
    private Long compteId;
    private String compteNom;
    private String numeroRib;
    private String nomBanque;
    private String numeroCompteComptable = "5141";
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private LocalDate dateArrete;

    private BigDecimal soldeReleve = BigDecimal.ZERO; // (A)
    private BigDecimal soldeComptable = BigDecimal.ZERO; // (G)
    private BigDecimal ecart = BigDecimal.ZERO;

    // Totaux État de Rapprochement Bancaire (A, B, C, D, E, F, G, H)
    private BigDecimal totalDebitReleveSuspens = BigDecimal.ZERO;   // (B)
    private BigDecimal totalCreditReleveSuspens = BigDecimal.ZERO;  // (C)
    private BigDecimal totalDebitComptaSuspens = BigDecimal.ZERO;   // (D)
    private BigDecimal totalCreditComptaSuspens = BigDecimal.ZERO;  // (E)
    private BigDecimal soldeTheoriqueComptable = BigDecimal.ZERO;   // (F)
    private BigDecimal soldeReelComptable = BigDecimal.ZERO;        // (G)
    private BigDecimal ecartConcordance = BigDecimal.ZERO;          // (H = G - F)

    private int totalLignesReleve = 0;
    private int totalRapprochees = 0;
    private int totalNonComptabilisees = 0; // ⚠ Opérations relevé en suspens
    private int totalEnAttenteBanque = 0;   // ⏳ Écritures compta en suspens

    private List<ItemComparatifRapprochementDTO> items = new ArrayList<>();
    private List<LigneEcritureSimpleDTO> ecrituresNonPointees = new ArrayList<>();

    public RapprochementComparatif5141DTO() {}

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

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getNumeroCompteComptable() {
        return numeroCompteComptable;
    }

    public void setNumeroCompteComptable(String numeroCompteComptable) {
        this.numeroCompteComptable = numeroCompteComptable;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public LocalDate getDateArrete() {
        return dateArrete != null ? dateArrete : dateFin;
    }

    public void setDateArrete(LocalDate dateArrete) {
        this.dateArrete = dateArrete;
        if (this.dateFin == null) {
            this.dateFin = dateArrete;
        }
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

    public int getTotalLignesReleve() {
        return totalLignesReleve;
    }

    public void setTotalLignesReleve(int totalLignesReleve) {
        this.totalLignesReleve = totalLignesReleve;
    }

    public int getTotalRapprochees() {
        return totalRapprochees;
    }

    public void setTotalRapprochees(int totalRapprochees) {
        this.totalRapprochees = totalRapprochees;
    }

    public int getTotalNonComptabilisees() {
        return totalNonComptabilisees;
    }

    public void setTotalNonComptabilisees(int totalNonComptabilisees) {
        this.totalNonComptabilisees = totalNonComptabilisees;
    }

    public int getTotalEnAttenteBanque() {
        return totalEnAttenteBanque;
    }

    public void setTotalEnAttenteBanque(int totalEnAttenteBanque) {
        this.totalEnAttenteBanque = totalEnAttenteBanque;
    }

    public List<ItemComparatifRapprochementDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemComparatifRapprochementDTO> items) {
        this.items = items;
    }

    public List<LigneEcritureSimpleDTO> getEcrituresNonPointees() {
        return ecrituresNonPointees;
    }

    public void setEcrituresNonPointees(List<LigneEcritureSimpleDTO> ecrituresNonPointees) {
        this.ecrituresNonPointees = ecrituresNonPointees;
    }

    public BigDecimal getTotalDebitReleveSuspens() {
        return totalDebitReleveSuspens;
    }

    public void setTotalDebitReleveSuspens(BigDecimal totalDebitReleveSuspens) {
        this.totalDebitReleveSuspens = totalDebitReleveSuspens;
    }

    public BigDecimal getTotalCreditReleveSuspens() {
        return totalCreditReleveSuspens;
    }

    public void setTotalCreditReleveSuspens(BigDecimal totalCreditReleveSuspens) {
        this.totalCreditReleveSuspens = totalCreditReleveSuspens;
    }

    public BigDecimal getTotalDebitComptaSuspens() {
        return totalDebitComptaSuspens;
    }

    public void setTotalDebitComptaSuspens(BigDecimal totalDebitComptaSuspens) {
        this.totalDebitComptaSuspens = totalDebitComptaSuspens;
    }

    public BigDecimal getTotalCreditComptaSuspens() {
        return totalCreditComptaSuspens;
    }

    public void setTotalCreditComptaSuspens(BigDecimal totalCreditComptaSuspens) {
        this.totalCreditComptaSuspens = totalCreditComptaSuspens;
    }

    public BigDecimal getSoldeTheoriqueComptable() {
        return soldeTheoriqueComptable;
    }

    public void setSoldeTheoriqueComptable(BigDecimal soldeTheoriqueComptable) {
        this.soldeTheoriqueComptable = soldeTheoriqueComptable;
    }

    public BigDecimal getSoldeReelComptable() {
        return soldeReelComptable;
    }

    public void setSoldeReelComptable(BigDecimal soldeReelComptable) {
        this.soldeReelComptable = soldeReelComptable;
    }

    public BigDecimal getEcartConcordance() {
        return ecartConcordance;
    }

    public void setEcartConcordance(BigDecimal ecartConcordance) {
        this.ecartConcordance = ecartConcordance;
    }
}
