package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.StatutLivraison;

import java.time.LocalDateTime;
import java.util.List;

public class LivraisonDTO {
    private Long id;
    private String numeroLivraison;
    private Long commandeId;
    private String commandeNumero;
    private LocalDateTime dateLivraison;
    private String transporteur;
    private String numeroSuivi;
    private String observations;
    private List<LigneLivraisonDTO> lignesLivraison;
    private StatutLivraison statut;
    private DepotDTO depot;

    // Constructors
    public LivraisonDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroLivraison() { return numeroLivraison; }
    public void setNumeroLivraison(String numeroLivraison) { this.numeroLivraison = numeroLivraison; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public String getCommandeNumero() { return commandeNumero; }
    public void setCommandeNumero(String commandeNumero) { this.commandeNumero = commandeNumero; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getTransporteur() { return transporteur; }
    public void setTransporteur(String transporteur) { this.transporteur = transporteur; }

    public String getNumeroSuivi() { return numeroSuivi; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public DepotDTO getDepot() {
        return depot;
    }

    public void setDepot(DepotDTO depot) {
        this.depot = depot;
    }

    public List<LigneLivraisonDTO> getLignesLivraison() { return lignesLivraison; }
    public void setLignesLivraison(List<LigneLivraisonDTO> lignesLivraison) { this.lignesLivraison = lignesLivraison; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }
}