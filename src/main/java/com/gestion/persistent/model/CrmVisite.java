package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutVisite;
import com.gestion.persistent.enums.TypeVisite;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_visites")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

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
    @JoinColumn(name = "opportunite_id")
    private CrmOpportunite opportunite;

    @Transient
    private Long opportuniteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_id", nullable = false)
    private User commercial;

    @Transient
    private Long commercialId;

    @Column(name = "date_visite", nullable = false)
    private LocalDateTime dateVisite;

    @Column(length = 255)
    private String lieu;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_visite", nullable = false, length = 50)
    private TypeVisite typeVisite = TypeVisite.PROSPECTION;

    @Column(length = 255)
    private String objectif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutVisite statut = StatutVisite.PLANIFIEE;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public CrmVisite() {}

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null && tenant > 0) ? tenant : 1L;
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        if (this.statut == null) {
            this.statut = StatutVisite.PLANIFIEE;
        }
        if (this.typeVisite == null) {
            this.typeVisite = TypeVisite.PROSPECTION;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public CrmEntreprise getEntreprise() { return entreprise; }
    public void setEntreprise(CrmEntreprise entreprise) { this.entreprise = entreprise; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public CrmContact getContact() { return contact; }
    public void setContact(CrmContact contact) { this.contact = contact; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public CrmOpportunite getOpportunite() { return opportunite; }
    public void setOpportunite(CrmOpportunite opportunite) { this.opportunite = opportunite; }

    public Long getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(Long opportuniteId) { this.opportuniteId = opportuniteId; }

    public User getCommercial() { return commercial; }
    public void setCommercial(User commercial) { this.commercial = commercial; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public LocalDateTime getDateVisite() { return dateVisite; }
    public void setDateVisite(LocalDateTime dateVisite) { this.dateVisite = dateVisite; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public TypeVisite getTypeVisite() { return typeVisite; }
    public void setTypeVisite(TypeVisite typeVisite) { this.typeVisite = typeVisite; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    public StatutVisite getStatut() { return statut; }
    public void setStatut(StatutVisite statut) { this.statut = statut; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
