package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReleveBancaireDTO {
    private Long id;
    private String referenceReleve;
    private Long compteFinancierId;
    private String compteFinancierNom;
    private String banqueNom;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal soldeInitial;
    private BigDecimal soldeFinal;
    private LocalDateTime dateImport;
    private String statut;
    private int totalLignes;
    private int lignesRapprochees;
    private List<LigneReleveBancaireDTO> lignes = new ArrayList<>();

    public ReleveBancaireDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceReleve() {
        return referenceReleve;
    }

    public void setReferenceReleve(String referenceReleve) {
        this.referenceReleve = referenceReleve;
    }

    public Long getCompteFinancierId() {
        return compteFinancierId;
    }

    public void setCompteFinancierId(Long compteFinancierId) {
        this.compteFinancierId = compteFinancierId;
    }

    public String getCompteFinancierNom() {
        return compteFinancierNom;
    }

    public void setCompteFinancierNom(String compteFinancierNom) {
        this.compteFinancierNom = compteFinancierNom;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
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

    public BigDecimal getSoldeInitial() {
        return soldeInitial;
    }

    public void setSoldeInitial(BigDecimal soldeInitial) {
        this.soldeInitial = soldeInitial;
    }

    public BigDecimal getSoldeFinal() {
        return soldeFinal;
    }

    public void setSoldeFinal(BigDecimal soldeFinal) {
        this.soldeFinal = soldeFinal;
    }

    public LocalDateTime getDateImport() {
        return dateImport;
    }

    public void setDateImport(LocalDateTime dateImport) {
        this.dateImport = dateImport;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public int getTotalLignes() {
        return totalLignes;
    }

    public void setTotalLignes(int totalLignes) {
        this.totalLignes = totalLignes;
    }

    public int getLignesRapprochees() {
        return lignesRapprochees;
    }

    public void setLignesRapprochees(int lignesRapprochees) {
        this.lignesRapprochees = lignesRapprochees;
    }

    public List<LigneReleveBancaireDTO> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneReleveBancaireDTO> lignes) {
        this.lignes = lignes;
    }
}
