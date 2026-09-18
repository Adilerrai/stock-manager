package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "collaborateurs_societes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "societe_id"})
})
public class CollaborateurSociete {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "societe_id", nullable = false)
    private Long societeId;

    @Column(name = "role_dossier", length = 50)
    private String roleDossier = "GESTIONNAIRE"; // RESPONSABLE_DOSSIER, GESTIONNAIRE, LECTEUR

    @Column(name = "date_affectation")
    private LocalDateTime dateAffectation = LocalDateTime.now();

    public CollaborateurSociete() {}

    public CollaborateurSociete(Long userId, Long societeId, String roleDossier) {
        this.userId = userId;
        this.societeId = societeId;
        this.roleDossier = roleDossier != null ? roleDossier : "GESTIONNAIRE";
        this.dateAffectation = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }

    public String getRoleDossier() { return roleDossier; }
    public void setRoleDossier(String roleDossier) { this.roleDossier = roleDossier; }

    public LocalDateTime getDateAffectation() { return dateAffectation; }
    public void setDateAffectation(LocalDateTime dateAffectation) { this.dateAffectation = dateAffectation; }
}
