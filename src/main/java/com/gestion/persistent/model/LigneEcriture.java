package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "lignes_ecriture")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LigneEcriture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ecriture_id", nullable = false)
    @JsonBackReference
    private EcritureComptable ecriture;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "compte_id", nullable = false)
    private CompteComptable compte;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal debit = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "libelle_ligne")
    private String libelleLigne;

    @Column(name = "reference_ligne")
    private String referenceLigne;

    @Column(length = 10)
    private String lettrage; // Lettrage pour rapprochement / tiers (ex: "AA")

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public LigneEcriture() {}

    public LigneEcriture(CompteComptable compte, BigDecimal debit, BigDecimal credit, String libelleLigne, Long pointDeVenteId) {
        this.compte = compte;
        this.debit = debit != null ? debit : BigDecimal.ZERO;
        this.credit = credit != null ? credit : BigDecimal.ZERO;
        this.libelleLigne = libelleLigne;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EcritureComptable getEcriture() {
        return ecriture;
    }

    public void setEcriture(EcritureComptable ecriture) {
        this.ecriture = ecriture;
    }

    public CompteComptable getCompte() {
        return compte;
    }

    public void setCompte(CompteComptable compte) {
        this.compte = compte;
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

    public String getLibelleLigne() {
        return libelleLigne;
    }

    public void setLibelleLigne(String libelleLigne) {
        this.libelleLigne = libelleLigne;
    }

    public String getReferenceLigne() {
        return referenceLigne;
    }

    public void setReferenceLigne(String referenceLigne) {
        this.referenceLigne = referenceLigne;
    }

    public String getLettrage() {
        return lettrage;
    }

    public void setLettrage(String lettrage) {
        this.lettrage = lettrage;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }
}
