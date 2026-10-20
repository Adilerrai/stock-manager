package com.gestion.persistent.dto;

public class CrmValiderRapportRequest {
    private boolean approuve;
    private String motifRejet;

    public CrmValiderRapportRequest() {}

    public boolean isApprouve() { return approuve; }
    public void setApprouve(boolean approuve) { this.approuve = approuve; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }
}
