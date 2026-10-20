package com.gestion.persistent.dto;

import com.gestion.persistent.enums.EtapeOpportunite;

public class CrmTransitionEtapeRequest {
    private EtapeOpportunite nouvelleEtape;
    private String motifPerte;
    private String notes;

    public CrmTransitionEtapeRequest() {}

    public EtapeOpportunite getNouvelleEtape() { return nouvelleEtape; }
    public void setNouvelleEtape(EtapeOpportunite nouvelleEtape) { this.nouvelleEtape = nouvelleEtape; }

    public String getMotifPerte() { return motifPerte; }
    public void setMotifPerte(String motifPerte) { this.motifPerte = motifPerte; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
