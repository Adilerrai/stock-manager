package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.SourceAcquisition;
import com.gestion.persistent.enums.StatutContact;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_contacts")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmContact {

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

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(name = "nom_complet", length = 200)
    private String nomComplet;

    @Column(length = 100)
    private String fonction;

    @Column(length = 150)
    private String email;

    @Column(length = 50)
    private String telephone;

    @Column(length = 50)
    private String mobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_acquisition", length = 50)
    private SourceAcquisition sourceAcquisition = SourceAcquisition.PROSPECTION_TERRAIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutContact statut = StatutContact.PROSPECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_responsable_id")
    private User commercialResponsable;

    @Transient
    private Long commercialResponsableId;

    @Column(nullable = false)
    private Boolean principal = false;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification = LocalDateTime.now();

    public CrmContact() {}

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null && tenant > 0) ? tenant : 1L;
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        this.dateModification = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutContact.PROSPECT;
        }
        if (this.principal == null) {
            this.principal = false;
        }
        updateNomComplet();
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
        updateNomComplet();
    }

    private void updateNomComplet() {
        if (this.prenom != null && !this.prenom.trim().isEmpty()) {
            this.nomComplet = this.prenom.trim() + " " + (this.nom != null ? this.nom.trim() : "");
        } else {
            this.nomComplet = this.nom != null ? this.nom.trim() : "";
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

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public SourceAcquisition getSourceAcquisition() { return sourceAcquisition; }
    public void setSourceAcquisition(SourceAcquisition sourceAcquisition) { this.sourceAcquisition = sourceAcquisition; }

    public StatutContact getStatut() { return statut; }
    public void setStatut(StatutContact statut) { this.statut = statut; }

    public User getCommercialResponsable() { return commercialResponsable; }
    public void setCommercialResponsable(User commercialResponsable) { this.commercialResponsable = commercialResponsable; }

    public Long getCommercialResponsableId() { return commercialResponsableId; }
    public void setCommercialResponsableId(Long commercialResponsableId) { this.commercialResponsableId = commercialResponsableId; }

    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
