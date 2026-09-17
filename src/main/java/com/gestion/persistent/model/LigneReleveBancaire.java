package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutRapprochement;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lignes_releves_bancaires")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneReleveBancaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "releve_bancaire_id", nullable = false)
    private ReleveBancaire releveBancaire;

    @Column(name = "date_operation", nullable = false)
    private LocalDate dateOperation;

    @Column(name = "date_valeur")
    private LocalDate dateValeur;

    @Column(nullable = false, length = 500)
    private String libelle;

    @Column(precision = 15, scale = 2)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_rapprochement", nullable = false)
    private StatutRapprochement statut = StatutRapprochement.NON_RAPPROCHE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mouvement_tresorerie_id")
    private MouvementTresorerie mouvementTresorerie;

    @Column(name = "ligne_ecriture_id")
    private Long ligneEcritureId;

    @Column(name = "date_rapprochement")
    private LocalDateTime dateRapprochement;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public LigneReleveBancaire() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReleveBancaire getReleveBancaire() {
        return releveBancaire;
    }

    public void setReleveBancaire(ReleveBancaire releveBancaire) {
        this.releveBancaire = releveBancaire;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit != null ? debit : BigDecimal.ZERO;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit != null ? credit : BigDecimal.ZERO;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public StatutRapprochement getStatut() {
        return statut;
    }

    public void setStatut(StatutRapprochement statut) {
        this.statut = statut;
    }

    public MouvementTresorerie getMouvementTresorerie() {
        return mouvementTresorerie;
    }

    public void setMouvementTresorerie(MouvementTresorerie mouvementTresorerie) {
        this.mouvementTresorerie = mouvementTresorerie;
    }

    public Long getLigneEcritureId() {
        return ligneEcritureId;
    }

    public void setLigneEcritureId(Long ligneEcritureId) {
        this.ligneEcritureId = ligneEcritureId;
    }

    public LocalDateTime getDateRapprochement() {
        return dateRapprochement;
    }

    public void setDateRapprochement(LocalDateTime dateRapprochement) {
        this.dateRapprochement = dateRapprochement;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }
}
