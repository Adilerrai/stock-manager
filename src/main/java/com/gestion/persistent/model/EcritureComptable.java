package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ecritures_comptables")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EcritureComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_piece", nullable = false, length = 50)
    private String numeroPiece;

    @Column(name = "date_ecriture", nullable = false)
    private LocalDate dateEcriture = LocalDate.now();

    @Column(nullable = false)
    private String libelle;

    @Column(name = "reference_piece")
    private String referencePiece; // ex: FACT-2026-0001, REG-004

    @Column(nullable = false)
    private Boolean validee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalComptable journal;

    @OneToMany(mappedBy = "ecriture", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneEcriture> lignes = new ArrayList<>();

    @Column(name = "total_debit", precision = 15, scale = 2)
    private BigDecimal totalDebit = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 15, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public EcritureComptable() {}

    public void recalculerTotaux() {
        this.totalDebit = BigDecimal.ZERO;
        this.totalCredit = BigDecimal.ZERO;
        if (this.lignes != null) {
            for (LigneEcriture ligne : this.lignes) {
                if (ligne.getDebit() != null) {
                    this.totalDebit = this.totalDebit.add(ligne.getDebit());
                }
                if (ligne.getCredit() != null) {
                    this.totalCredit = this.totalCredit.add(ligne.getCredit());
                }
            }
        }
    }

    public boolean isEquilibree() {
        recalculerTotaux();
        return this.totalDebit != null && this.totalCredit != null &&
               this.totalDebit.compareTo(this.totalCredit) == 0;
    }

    public void addLigne(LigneEcriture ligne) {
        if (lignes == null) {
            lignes = new ArrayList<>();
        }
        lignes.add(ligne);
        ligne.setEcriture(this);
        recalculerTotaux();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroPiece() {
        return numeroPiece;
    }

    public void setNumeroPiece(String numeroPiece) {
        this.numeroPiece = numeroPiece;
    }

    public LocalDate getDateEcriture() {
        return dateEcriture;
    }

    public void setDateEcriture(LocalDate dateEcriture) {
        this.dateEcriture = dateEcriture;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getReferencePiece() {
        return referencePiece;
    }

    public void setReferencePiece(String referencePiece) {
        this.referencePiece = referencePiece;
    }

    public Boolean getValidee() {
        return validee;
    }

    public void setValidee(Boolean validee) {
        this.validee = validee;
    }

    public JournalComptable getJournal() {
        return journal;
    }

    public void setJournal(JournalComptable journal) {
        this.journal = journal;
    }

    public List<LigneEcriture> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneEcriture> lignes) {
        this.lignes = lignes;
        recalculerTotaux();
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
