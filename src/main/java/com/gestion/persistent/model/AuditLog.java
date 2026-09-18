package com.gestion.persistent.model;

import com.gestion.persistent.enums.ActionAudit;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_entite", columnList = "entite, entite_id"),
    @Index(name = "idx_audit_utilisateur", columnList = "utilisateur"),
    @Index(name = "idx_audit_date", columnList = "date_action"),
    @Index(name = "idx_audit_tenant", columnList = "point_de_vente_id")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom de l'entité concernée (ex: "EcritureComptable", "Immobilisation") */
    @Column(nullable = false, length = 100)
    private String entite;

    /** ID de l'objet modifié */
    @Column(name = "entite_id", nullable = false)
    private Long entiteId;

    /** Type d'action effectuée */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ActionAudit action;

    /** Champ modifié (null pour CREATION/SUPPRESSION) */
    @Column(name = "champ_modifie", length = 100)
    private String champModifie;

    /** Valeur avant modification */
    @Column(name = "ancienne_valeur", columnDefinition = "TEXT")
    private String ancienneValeur;

    /** Valeur après modification */
    @Column(name = "nouvelle_valeur", columnDefinition = "TEXT")
    private String nouvelleValeur;

    /** Description lisible de l'action */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Login ou nom de l'utilisateur ayant effectué l'action */
    @Column(nullable = false, length = 150)
    private String utilisateur;

    /** Horodatage de l'action */
    @Column(name = "date_action", nullable = false)
    private LocalDateTime dateAction = LocalDateTime.now();

    /** Adresse IP du client (optionnel) */
    @Column(name = "adresse_ip", length = 50)
    private String adresseIp;

    /** Tenant */
    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public AuditLog() {}

    // ---- Builder-style factory methods ----

    public static AuditLog creation(String entite, Long entiteId, String description, String utilisateur, Long tenantId) {
        AuditLog log = new AuditLog();
        log.entite = entite;
        log.entiteId = entiteId;
        log.action = ActionAudit.CREATION;
        log.description = description;
        log.utilisateur = utilisateur;
        log.pointDeVenteId = tenantId;
        return log;
    }

    public static AuditLog modification(String entite, Long entiteId, String champModifie,
                                         String ancienneValeur, String nouvelleValeur,
                                         String description, String utilisateur, Long tenantId) {
        AuditLog log = new AuditLog();
        log.entite = entite;
        log.entiteId = entiteId;
        log.action = ActionAudit.MODIFICATION;
        log.champModifie = champModifie;
        log.ancienneValeur = ancienneValeur;
        log.nouvelleValeur = nouvelleValeur;
        log.description = description;
        log.utilisateur = utilisateur;
        log.pointDeVenteId = tenantId;
        return log;
    }

    public static AuditLog action(ActionAudit action, String entite, Long entiteId,
                                   String description, String utilisateur, Long tenantId) {
        AuditLog log = new AuditLog();
        log.entite = entite;
        log.entiteId = entiteId;
        log.action = action;
        log.description = description;
        log.utilisateur = utilisateur;
        log.pointDeVenteId = tenantId;
        return log;
    }

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

    public String getAdresseIp() { return adresseIp; }
    public void setAdresseIp(String adresseIp) { this.adresseIp = adresseIp; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
