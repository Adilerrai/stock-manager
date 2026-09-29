package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class ModeleEcritureDTO {

    private String code; // "LOYER_COMMERCIAL", "HONORAIRES_RAS", "LEASING", "SALAIRES", etc.
    private String libelle;
    private String description;
    private String journalParDefaut; // "AC", "OD", "BQ", etc.
    private String categorie; // "IMMOBILIER", "SOCIAL", "FINANCIER", "EXPLOITATION"
    private boolean actif = true;

    private List<LigneModeleEcritureDTO> lignes = new ArrayList<>();

    public ModeleEcritureDTO() {}

    public ModeleEcritureDTO(String code, String libelle, String description, String journalParDefaut, String categorie) {
        this.code = code;
        this.libelle = libelle;
        this.description = description;
        this.journalParDefaut = journalParDefaut;
        this.categorie = categorie;
        this.actif = true;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getJournalParDefaut() { return journalParDefaut; }
    public void setJournalParDefaut(String journalParDefaut) { this.journalParDefaut = journalParDefaut; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public List<LigneModeleEcritureDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneModeleEcritureDTO> lignes) { this.lignes = lignes; }
}
