package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeActiviteCrm;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_activites")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmActivite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_activite", nullable = false, length = 50)
    private TypeActiviteCrm typeActivite;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

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
    @JoinColumn(name = "visite_id")
    private CrmVisite visite;

    @Transient
    private Long visiteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Transient
    private Long userId;

    @Column(name = "date_activite", nullable = false)
    private LocalDateTime dateActivite = LocalDateTime.now();

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public CrmActivite() {}

    public CrmActivite(TypeActiviteCrm typeActivite, String titre, String description) {
        this.typeActivite = typeActivite;
        this.titre = titre;
        this.description = description;
        this.dateActivite = LocalDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null && tenant > 0) ? tenant : 1L;
        }
        if (this.dateActivite == null) {
            this.dateActivite = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public TypeActiviteCrm getTypeActivite() { return typeActivite; }
    public void setTypeActivite(TypeActiviteCrm typeActivite) { this.typeActivite = typeActivite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public CrmVisite getVisite() { return visite; }
    public void setVisite(CrmVisite visite) { this.visite = visite; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getDateActivite() { return dateActivite; }
    public void setDateActivite(LocalDateTime dateActivite) { this.dateActivite = dateActivite; }

    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
}
