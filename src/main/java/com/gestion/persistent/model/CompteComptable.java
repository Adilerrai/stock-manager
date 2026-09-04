package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.SensCompte;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comptes_comptables")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CompteComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_compte", nullable = false, length = 20)
    private String numeroCompte;

    @Column(nullable = false)
    private String libelle;

    @Column(nullable = false)
    private Integer classe; // 1 à 8

    @Enumerated(EnumType.STRING)
    @Column(name = "sens_par_defaut")
    private SensCompte sensParDefaut = SensCompte.DEBIT;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public CompteComptable() {}

    public CompteComptable(String numeroCompte, String libelle, Integer classe, SensCompte sensParDefaut, Long pointDeVenteId) {
        this.numeroCompte = numeroCompte;
        this.libelle = libelle;
        this.classe = classe;
        this.sensParDefaut = sensParDefaut;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
        this.actif = true;
        this.dateCreation = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(String numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Integer getClasse() {
        return classe;
    }

    public void setClasse(Integer classe) {
        this.classe = classe;
    }

    public SensCompte getSensParDefaut() {
        return sensParDefaut;
    }

    public void setSensParDefaut(SensCompte sensParDefaut) {
        this.sensParDefaut = sensParDefaut;
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
