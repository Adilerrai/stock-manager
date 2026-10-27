package com.gestion.persistent.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ConvertirPrecommandesRequest {
    private List<Long> ligneDevisIds;
    private Long targetDemandeAchatId;
    private String demandeurNom;
    private String serviceDemandeur;
    private String objet;
    private LocalDateTime dateSouhaitee;
    private String observations;

    public ConvertirPrecommandesRequest() {}

    public List<Long> getLigneDevisIds() { return ligneDevisIds; }
    public void setLigneDevisIds(List<Long> ligneDevisIds) { this.ligneDevisIds = ligneDevisIds; }

    public Long getTargetDemandeAchatId() { return targetDemandeAchatId; }
    public void setTargetDemandeAchatId(Long targetDemandeAchatId) { this.targetDemandeAchatId = targetDemandeAchatId; }

    public String getDemandeurNom() { return demandeurNom; }
    public void setDemandeurNom(String demandeurNom) { this.demandeurNom = demandeurNom; }

    public String getServiceDemandeur() { return serviceDemandeur; }
    public void setServiceDemandeur(String serviceDemandeur) { this.serviceDemandeur = serviceDemandeur; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public LocalDateTime getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDateTime dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
