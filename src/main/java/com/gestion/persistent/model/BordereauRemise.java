package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutRemise;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bordereaux_remise")
public class BordereauRemise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroBordereau;

    @Column(name = "date_remise", nullable = false)
    private LocalDate dateRemise = LocalDate.now();

    @Column(name = "nom_banque", nullable = false)
    private String nomBanque;

    @Column(name = "compte_bancaire")
    private String compteBancaire;

    @Column(name = "type_valeur", nullable = false)
    private String typeValeur = "CHEQUE"; // CHEQUE ou EFFET

    @Column(name = "montant_total", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "nombre_valeurs")
    private Integer nombreValeurs = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRemise statut = StatutRemise.BROUILLON;

    private String notes;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public BordereauRemise() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroBordereau() {
        return numeroBordereau;
    }

    public void setNumeroBordereau(String numeroBordereau) {
        this.numeroBordereau = numeroBordereau;
    }

    public LocalDate getDateRemise() {
        return dateRemise;
    }

    public void setDateRemise(LocalDate dateRemise) {
        this.dateRemise = dateRemise;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getCompteBancaire() {
        return compteBancaire;
    }

    public void setCompteBancaire(String compteBancaire) {
        this.compteBancaire = compteBancaire;
    }

    public String getTypeValeur() {
        return typeValeur;
    }

    public void setTypeValeur(String typeValeur) {
        this.typeValeur = typeValeur;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public Integer getNombreValeurs() {
        return nombreValeurs;
    }

    public void setNombreValeurs(Integer nombreValeurs) {
        this.nombreValeurs = nombreValeurs;
    }

    public StatutRemise getStatut() {
        return statut;
    }

    public void setStatut(StatutRemise statut) {
        this.statut = statut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
