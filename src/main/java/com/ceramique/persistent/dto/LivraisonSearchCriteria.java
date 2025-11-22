package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.StatutLivraison;
import java.time.LocalDateTime;

public class LivraisonSearchCriteria {
    private String numeroLivraison;
    private String numeroSuivi;
    private Long commandeId;
    private String transporteur;
    private StatutLivraison statut;
    private LocalDateTime dateDebutLivraison;
    private LocalDateTime dateFinLivraison;

    // Constructors
    public LivraisonSearchCriteria() {}

    // Getters et Setters
    public String getNumeroLivraison() { return numeroLivraison; }
    public void setNumeroLivraison(String numeroLivraison) { this.numeroLivraison = numeroLivraison; }

    public String getNumeroSuivi() { return numeroSuivi; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public String getTransporteur() { return transporteur; }
    public void setTransporteur(String transporteur) { this.transporteur = transporteur; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public LocalDateTime getDateDebutLivraison() { return dateDebutLivraison; }
    public void setDateDebutLivraison(LocalDateTime dateDebutLivraison) { this.dateDebutLivraison = dateDebutLivraison; }

    public LocalDateTime getDateFinLivraison() { return dateFinLivraison; }
    public void setDateFinLivraison(LocalDateTime dateFinLivraison) { this.dateFinLivraison = dateFinLivraison; }
}