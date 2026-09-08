package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeCompteFinancier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "comptes_financiers")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class CompteFinancier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCompteFinancier type;

    @Column(name = "solde_actuel", precision = 15, scale = 2, nullable = false)
    private BigDecimal soldeActuel = BigDecimal.ZERO;

    @Column(length = 10, nullable = false)
    private String devise = "MAD";

    @Column(name = "numero_compte_rib")
    private String numeroCompteRib;

    @Column(name = "nom_banque")
    private String nomBanque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id")
    private Banque banque;

    @Column(length = 150)
    private String agence; // Ex: Agence Maarif, Agence Hassan II

    @Column(name = "code_agence", length = 50)
    private String codeAgence;

    @Column(length = 150)
    private String titulaire;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public CompteFinancier() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public TypeCompteFinancier getType() {
        return type;
    }

    public void setType(TypeCompteFinancier type) {
        this.type = type;
    }

    public BigDecimal getSoldeActuel() {
        return soldeActuel;
    }

    public void setSoldeActuel(BigDecimal soldeActuel) {
        this.soldeActuel = soldeActuel;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public String getNumeroCompteRib() {
        return numeroCompteRib;
    }

    public void setNumeroCompteRib(String numeroCompteRib) {
        this.numeroCompteRib = numeroCompteRib;
    }

    public String getNomBanque() {
        if (banque != null && banque.getNom() != null) {
            return banque.getNom();
        }
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public String getAgence() {
        return agence;
    }

    public void setAgence(String agence) {
        this.agence = agence;
    }

    public String getCodeAgence() {
        return codeAgence;
    }

    public void setCodeAgence(String codeAgence) {
        this.codeAgence = codeAgence;
    }

    public String getTitulaire() {
        return titulaire;
    }

    public void setTitulaire(String titulaire) {
        this.titulaire = titulaire;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
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
