package com.gestion.persistent.dto;

public class RapprochementPointageRequest {
    private Long ligneReleveId;
    private Long mouvementTresorerieId;
    private Long ligneEcritureId;

    public RapprochementPointageRequest() {}

    public Long getLigneReleveId() {
        return ligneReleveId;
    }

    public void setLigneReleveId(Long ligneReleveId) {
        this.ligneReleveId = ligneReleveId;
    }

    public Long getMouvementTresorerieId() {
        return mouvementTresorerieId;
    }

    public void setMouvementTresorerieId(Long mouvementTresorerieId) {
        this.mouvementTresorerieId = mouvementTresorerieId;
    }

    public Long getLigneEcritureId() {
        return ligneEcritureId;
    }

    public void setLigneEcritureId(Long ligneEcritureId) {
        this.ligneEcritureId = ligneEcritureId;
    }
}
