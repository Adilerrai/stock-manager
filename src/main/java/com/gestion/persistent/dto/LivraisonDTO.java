package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutLivraison;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LivraisonDTO {
    private Long id;
    private String numeroLivraison;
    private Long commandeId;
    private String commandeNumero;
    private Long fournisseurId;
    private String fournisseurNom;
    private LocalDateTime dateLivraison;
    private String transporteur;
    private String numeroSuivi;
    private String observations;
    private List<LigneLivraisonDTO> lignesLivraison;
    private StatutLivraison statut;
    private DepotDTO depot;
    private Long depotId;
    private String depotNom;

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

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getTransporteur() { return transporteur; }
    public void setTransporteur(String transporteur) { this.transporteur = transporteur; }

    public String getNumeroSuivi() { return numeroSuivi; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public DepotDTO getDepot() {
        if (depot != null) return depot;
        if (lignesLivraison != null && !lignesLivraison.isEmpty() && lignesLivraison.get(0).getDepot() != null) {
            return lignesLivraison.get(0).getDepot();
        }
        return null;
    }

    public void setDepot(DepotDTO depot) {
        this.depot = depot;
    }

    public Long getDepotId() {
        if (depotId != null) return depotId;
        if (depot != null && depot.getId() != null) return depot.getId();
        if (lignesLivraison != null && !lignesLivraison.isEmpty() && lignesLivraison.get(0).getDepot() != null) {
            return lignesLivraison.get(0).getDepot().getId();
        }
        return null;
    }

    public void setDepotId(Long depotId) {
        this.depotId = depotId;
    }

    public String getDepotNom() {
        if (depotNom != null) return depotNom;
        if (depot != null && depot.getNom() != null) return depot.getNom();
        if (lignesLivraison != null && !lignesLivraison.isEmpty() && lignesLivraison.get(0).getDepot() != null) {
            return lignesLivraison.get(0).getDepot().getNom();
        }
        return null;
    }

    public void setDepotNom(String depotNom) {
        this.depotNom = depotNom;
    }

    public List<LigneLivraisonDTO> getLignesLivraison() { return lignesLivraison; }
    public void setLignesLivraison(List<LigneLivraisonDTO> lignesLivraison) { this.lignesLivraison = lignesLivraison; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }
}

