package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "releves_bancaires")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ReleveBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_releve", nullable = false)
    private String referenceReleve;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_financier_id", nullable = false)
    private CompteFinancier compteFinancier;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Column(name = "solde_initial", precision = 15, scale = 2)
    private BigDecimal soldeInitial = BigDecimal.ZERO;

    @Column(name = "solde_final", precision = 15, scale = 2)
    private BigDecimal soldeFinal = BigDecimal.ZERO;

    @Column(name = "date_import")
    private LocalDateTime dateImport = LocalDateTime.now();

    @Column(length = 30)
    private String statut = "EN_COURS"; // EN_COURS, CLOTURE

    @OneToMany(mappedBy = "releveBancaire", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LigneReleveBancaire> lignes = new ArrayList<>();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public ReleveBancaire() {}

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

    public CompteFinancier getCompteFinancier() {
        return compteFinancier;
    }

    public void setCompteFinancier(CompteFinancier compteFinancier) {
        this.compteFinancier = compteFinancier;
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

    public List<LigneReleveBancaire> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneReleveBancaire> lignes) {
        this.lignes = lignes;
    }

    public void addLigne(LigneReleveBancaire ligne) {
        lignes.add(ligne);
        ligne.setReleveBancaire(this);
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }
}
