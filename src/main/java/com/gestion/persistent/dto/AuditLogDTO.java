package com.gestion.persistent.dto;

import com.gestion.persistent.enums.ActionAudit;

import java.time.LocalDateTime;

public class AuditLogDTO {

    private Long id;
    private String entite;
    private Long entiteId;
    private ActionAudit action;
    private String champModifie;
    private String ancienneValeur;
    private String nouvelleValeur;
    private String description;
    private String utilisateur;
    private LocalDateTime dateAction;

    public AuditLogDTO() {}

    // ---- Getters & Setters ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEntite() { return entite; }
    public void setEntite(String entite) { this.entite = entite; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public ActionAudit getAction() { return action; }
    public void setAction(ActionAudit action) { this.action = action; }

    public String getChampModifie() { return champModifie; }
    public void setChampModifie(String champModifie) { this.champModifie = champModifie; }

    public String getAncienneValeur() { return ancienneValeur; }
    public void setAncienneValeur(String ancienneValeur) { this.ancienneValeur = ancienneValeur; }

    public String getNouvelleValeur() { return nouvelleValeur; }
    public void setNouvelleValeur(String nouvelleValeur) { this.nouvelleValeur = nouvelleValeur; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUtilisateur() { return utilisateur; }
    public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }

    public LocalDateTime getDateAction() { return dateAction; }
    public void setDateAction(LocalDateTime dateAction) { this.dateAction = dateAction; }
}
