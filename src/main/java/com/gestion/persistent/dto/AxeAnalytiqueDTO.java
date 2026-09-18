package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class AxeAnalytiqueDTO {
    private Long id;
    private String code;
    private String libelle;
    private String description;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private int nombreCentres;

    public AxeAnalytiqueDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public int getNombreCentres() { return nombreCentres; }
    public void setNombreCentres(int nombreCentres) { this.nombreCentres = nombreCentres; }
}
