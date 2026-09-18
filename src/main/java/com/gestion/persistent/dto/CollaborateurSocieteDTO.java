package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class CollaborateurSocieteDTO {

    private Long id;
    private Long userId;
    private String nomComplet;
    private String email;
    private Long societeId;
    private String raisonSociale;
    private String roleDossier;
    private LocalDateTime dateAffectation;

    public CollaborateurSocieteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getRoleDossier() { return roleDossier; }
    public void setRoleDossier(String roleDossier) { this.roleDossier = roleDossier; }

    public LocalDateTime getDateAffectation() { return dateAffectation; }
    public void setDateAffectation(LocalDateTime dateAffectation) { this.dateAffectation = dateAffectation; }
}
