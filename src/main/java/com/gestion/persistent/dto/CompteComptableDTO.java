package com.gestion.persistent.dto;

import com.gestion.persistent.enums.SensCompte;

public class CompteComptableDTO {
    private Long id;
    private String numeroCompte;
    private String libelle;
    private Integer classe;
    private SensCompte sensParDefaut;
    private Boolean actif;

    public CompteComptableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public Integer getClasse() { return classe; }
    public void setClasse(Integer classe) { this.classe = classe; }

    public SensCompte getSensParDefaut() { return sensParDefaut; }
    public void setSensParDefaut(SensCompte sensParDefaut) { this.sensParDefaut = sensParDefaut; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}
