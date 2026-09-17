package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutExercice;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exercices_comptables")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ExerciceComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutExercice statut = StatutExercice.OUVERT;

    @Column(name = "resultat_net", precision = 15, scale = 2)
    private BigDecimal resultatNet;

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @Column(name = "cloture_par", length = 100)
    private String cloturePar;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public ExerciceComptable() {}

    public ExerciceComptable(String code, String libelle, LocalDate dateDebut, LocalDate dateFin, Long pointDeVenteId) {
        this.code = code;
        this.libelle = libelle;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
        this.statut = StatutExercice.OUVERT;
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

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
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

    public StatutExercice getStatut() {
        return statut;
    }

    public void setStatut(StatutExercice statut) {
        this.statut = statut;
    }

    public BigDecimal getResultatNet() {
        return resultatNet;
    }

    public void setResultatNet(BigDecimal resultatNet) {
        this.resultatNet = resultatNet;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public String getCloturePar() {
        return cloturePar;
    }

    public void setCloturePar(String cloturePar) {
        this.cloturePar = cloturePar;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }
}
