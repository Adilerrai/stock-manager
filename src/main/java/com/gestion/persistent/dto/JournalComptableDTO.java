package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeJournal;

public class JournalComptableDTO {
    private Long id;
    private String code;
    private String libelle;
    private TypeJournal typeJournal;
    private Boolean actif;

    public JournalComptableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public TypeJournal getTypeJournal() { return typeJournal; }
    public void setTypeJournal(TypeJournal typeJournal) { this.typeJournal = typeJournal; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}
