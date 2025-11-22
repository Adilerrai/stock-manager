package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.StatutCommande;
import java.time.LocalDateTime;

public class CommandeSearchCriteria {
    private String numeroCommande;
    private Long fournisseurId;
    private StatutCommande statut;
    private LocalDateTime dateDebutCommande;
    private LocalDateTime dateFinCommande;
    private LocalDateTime dateDebutLivraison;
    private LocalDateTime dateFinLivraison;

    // Getters et Setters
    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateDebutCommande() { return dateDebutCommande; }
    public void setDateDebutCommande(LocalDateTime dateDebutCommande) { this.dateDebutCommande = dateDebutCommande; }

    public LocalDateTime getDateFinCommande() { return dateFinCommande; }
    public void setDateFinCommande(LocalDateTime dateFinCommande) { this.dateFinCommande = dateFinCommande; }

    public LocalDateTime getDateDebutLivraison() { return dateDebutLivraison; }
    public void setDateDebutLivraison(LocalDateTime dateDebutLivraison) { this.dateDebutLivraison = dateDebutLivraison; }

    public LocalDateTime getDateFinLivraison() { return dateFinLivraison; }
    public void setDateFinLivraison(LocalDateTime dateFinLivraison) { this.dateFinLivraison = dateFinLivraison; }
}