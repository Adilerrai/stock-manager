package com.gestion.persistent.dto;

public class CreerEcritureReleveRequest {
    private Long ligneReleveId;
    private Long compteContrepartieId;
    private String numeroCompteContrepartie;
    private String libelle;

    public CreerEcritureReleveRequest() {}

    public Long getLigneReleveId() {
        return ligneReleveId;
    }

    public void setLigneReleveId(Long ligneReleveId) {
        this.ligneReleveId = ligneReleveId;
    }

    public Long getCompteContrepartieId() {
        return compteContrepartieId;
    }

    public void setCompteContrepartieId(Long compteContrepartieId) {
        this.compteContrepartieId = compteContrepartieId;
    }

    public String getNumeroCompteContrepartie() {
        return numeroCompteContrepartie;
    }

    public void setNumeroCompteContrepartie(String numeroCompteContrepartie) {
        this.numeroCompteContrepartie = numeroCompteContrepartie;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
