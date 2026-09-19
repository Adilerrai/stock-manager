package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;

public class ReceptionCommandeDTO {
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateLivraison;
    private String transporteur;
    private String numeroSuivi;
    private String observations;
    private Long depotId;
    private List<LigneReceptionDTO> lignes;

    public ReceptionCommandeDTO() {}

    public LocalDateTime getDateLivraison() {
        return dateLivraison;
    }

    public void setDateLivraison(LocalDateTime dateLivraison) {
        this.dateLivraison = dateLivraison;
    }

    public String getTransporteur() {
        return transporteur;
    }

    public void setTransporteur(String transporteur) {
        this.transporteur = transporteur;
    }

    public String getNumeroSuivi() {
        return numeroSuivi;
    }

    public void setNumeroSuivi(String numeroSuivi) {
        this.numeroSuivi = numeroSuivi;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Long getDepotId() {
        return depotId;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public List<LigneReceptionDTO> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneReceptionDTO> lignes) {
        this.lignes = lignes;
    }
}
