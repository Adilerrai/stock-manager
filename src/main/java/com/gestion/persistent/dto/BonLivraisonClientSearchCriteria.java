package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutLivraison;
import java.time.LocalDateTime;

public class BonLivraisonClientSearchCriteria {
    private String numeroBl;
    private String numeroBL;
    private String numeroCommande;
    private String numeroCommandeClient;
    private String numeroCommandeExterne;
    private String bcClient;
    private String clientNom;
    private Long clientId;
    private Long commandeClientId;
    private Long livreurId;
    private StatutLivraison statut;
    private Boolean facturee;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Long societeId;
    private Boolean inclureAnnulees;

    public BonLivraisonClientSearchCriteria() {}

    public Boolean getInclureAnnulees() { return inclureAnnulees; }
    public void setInclureAnnulees(Boolean inclureAnnulees) { this.inclureAnnulees = inclureAnnulees; }

    public String getNumeroBl() {
        if (numeroBl != null && !numeroBl.trim().isEmpty()) {
            return numeroBl;
        }
        return numeroBL;
    }
    public void setNumeroBl(String numeroBl) {
        this.numeroBl = numeroBl;
        if (this.numeroBL == null) {
            this.numeroBL = numeroBl;
        }
    }

    public String getNumeroBL() {
        return getNumeroBl();
    }
    public void setNumeroBL(String numeroBL) {
        this.numeroBL = numeroBL;
        if (this.numeroBl == null) {
            this.numeroBl = numeroBL;
        }
    }

    public String getNumeroCommande() {
        if (numeroCommande != null && !numeroCommande.trim().isEmpty()) {
            return numeroCommande;
        }
        return numeroCommandeClient;
    }
    public void setNumeroCommande(String numeroCommande) {
        this.numeroCommande = numeroCommande;
        if (this.numeroCommandeClient == null) {
            this.numeroCommandeClient = numeroCommande;
        }
    }

    public String getNumeroCommandeClient() {
        return getNumeroCommande();
    }
    public void setNumeroCommandeClient(String numeroCommandeClient) {
        this.numeroCommandeClient = numeroCommandeClient;
        if (this.numeroCommande == null) {
            this.numeroCommande = numeroCommandeClient;
        }
    }

    public String getNumeroCommandeExterne() {
        if (numeroCommandeExterne != null && !numeroCommandeExterne.trim().isEmpty()) {
            return numeroCommandeExterne;
        }
        return bcClient;
    }
    public void setNumeroCommandeExterne(String numeroCommandeExterne) {
        this.numeroCommandeExterne = numeroCommandeExterne;
        if (this.bcClient == null) {
            this.bcClient = numeroCommandeExterne;
        }
    }

    public String getBcClient() {
        return getNumeroCommandeExterne();
    }
    public void setBcClient(String bcClient) {
        this.bcClient = bcClient;
        if (this.numeroCommandeExterne == null) {
            this.numeroCommandeExterne = bcClient;
        }
    }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getCommandeClientId() { return commandeClientId; }
    public void setCommandeClientId(Long commandeClientId) { this.commandeClientId = commandeClientId; }

    public Long getLivreurId() { return livreurId; }
    public void setLivreurId(Long livreurId) { this.livreurId = livreurId; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public Boolean getFacturee() { return facturee; }
    public void setFacturee(Boolean facturee) { this.facturee = facturee; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }
}
