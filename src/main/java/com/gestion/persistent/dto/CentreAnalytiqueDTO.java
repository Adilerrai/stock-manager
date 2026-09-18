package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class CentreAnalytiqueDTO {
    private Long id;
    private Long axeId;
    private String axeLibelle;
    private String code;
    private String libelle;
    private String type; // COUT, PROFIT, MIXTE
    private String responsable;
    private Boolean actif;
    private LocalDateTime dateCreation;

    public CentreAnalytiqueDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAxeId() { return axeId; }
    public void setAxeId(Long axeId) { this.axeId = axeId; }

    public String getAxeLibelle() { return axeLibelle; }
    public void setAxeLibelle(String axeLibelle) { this.axeLibelle = axeLibelle; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
