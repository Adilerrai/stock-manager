package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.PrioriteTache;
import com.gestion.persistent.enums.StatutTache;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_taches")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmTache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visite_id")
    private CrmVisite visite;

    @Transient
    private Long visiteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapport_id")
    private CrmRapportVisite rapport;

    @Transient
    private Long rapportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunite_id")
    private CrmOpportunite opportunite;

    @Transient
    private Long opportuniteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private CrmEntreprise entreprise;

    @Transient
    private Long entrepriseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private CrmContact contact;

    @Transient
    private Long contactId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigne_a_user_id", nullable = false)
    private User assigneA;

    @Transient
    private Long assigneAUserId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @Transient
    private Long creeParUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PrioriteTache priorite = PrioriteTache.MOYENNE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutTache statut = StatutTache.A_FAIRE;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "date_realisation")
    private LocalDateTime dateRealisation;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public CrmTache() {}

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null && tenant > 0) ? tenant : 1L;
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.priorite == null) {
            this.priorite = PrioriteTache.MOYENNE;
        }
        if (this.statut == null) {
            this.statut = StatutTache.A_FAIRE;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CrmVisite getVisite() { return visite; }
    public void setVisite(CrmVisite visite) { this.visite = visite; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public CrmRapportVisite getRapport() { return rapport; }
    public void setRapport(CrmRapportVisite rapport) { this.rapport = rapport; }

    public Long getRapportId() { return rapportId; }
    public void setRapportId(Long rapportId) { this.rapportId = rapportId; }

    public CrmOpportunite getOpportunite() { return opportunite; }
    public void setOpportunite(CrmOpportunite opportunite) { this.opportunite = opportunite; }

    public Long getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(Long opportuniteId) { this.opportuniteId = opportuniteId; }

    public CrmEntreprise getEntreprise() { return entreprise; }
    public void setEntreprise(CrmEntreprise entreprise) { this.entreprise = entreprise; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public CrmContact getContact() { return contact; }
    public void setContact(CrmContact contact) { this.contact = contact; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public User getAssigneA() { return assigneA; }
    public void setAssigneA(User assigneA) { this.assigneA = assigneA; }

    public Long getAssigneAUserId() { return assigneAUserId; }
    public void setAssigneAUserId(Long assigneAUserId) { this.assigneAUserId = assigneAUserId; }

    public User getCreePar() { return creePar; }
    public void setCreePar(User creePar) { this.creePar = creePar; }

    public Long getCreeParUserId() { return creeParUserId; }
    public void setCreeParUserId(Long creeParUserId) { this.creeParUserId = creeParUserId; }

    public PrioriteTache getPriorite() { return priorite; }
    public void setPriorite(PrioriteTache priorite) { this.priorite = priorite; }

    public StatutTache getStatut() { return statut; }
    public void setStatut(StatutTache statut) { this.statut = statut; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public LocalDateTime getDateRealisation() { return dateRealisation; }
    public void setDateRealisation(LocalDateTime dateRealisation) { this.dateRealisation = dateRealisation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
