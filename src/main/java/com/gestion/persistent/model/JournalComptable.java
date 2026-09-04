package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeJournal;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journaux_comptables")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class JournalComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code; // VT, AC, BQ, CA, OD

    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_journal", nullable = false)
    private TypeJournal typeJournal = TypeJournal.OPERATIONS_DIVERSES;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public JournalComptable() {}

    public JournalComptable(String code, String libelle, TypeJournal typeJournal, Long pointDeVenteId) {
        this.code = code;
        this.libelle = libelle;
        this.typeJournal = typeJournal;
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

    public TypeJournal getTypeJournal() {
        return typeJournal;
    }

    public void setTypeJournal(TypeJournal typeJournal) {
        this.typeJournal = typeJournal;
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
