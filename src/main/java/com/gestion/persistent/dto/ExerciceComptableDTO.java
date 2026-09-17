package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutExercice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExerciceComptableDTO {
    private Long id;
    private String code;
    private String libelle;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private StatutExercice statut;
    private BigDecimal resultatNet;
    private LocalDateTime dateCloture;
    private String cloturePar;
    private int nombreEcritures;

    public ExerciceComptableDTO() {}

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

    public int getNombreEcritures() {
        return nombreEcritures;
    }

    public void setNombreEcritures(int nombreEcritures) {
        this.nombreEcritures = nombreEcritures;
    }
}
