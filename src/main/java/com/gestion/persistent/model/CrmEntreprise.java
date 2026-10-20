package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutEntreprise;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_entreprises")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "secteur_activite", length = 100)
    private String secteurActivite;

    @Column(name = "taille_entreprise", length = 50)
    private String tailleEntreprise;

    @Column(columnDefinition = "TEXT")
    private String adresse;

    @Column(length = 100)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(length = 50)
    private String pays = "Maroc";

    @Column(length = 50)
    private String telephone;

    @Column(length = 150)
    private String email;

    @Column(name = "site_web", length = 200)
    private String siteWeb;

    @Column(length = 50)
    private String ice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutEntreprise statut = StatutEntreprise.PROSPECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_responsable_id")
    private User commercialResponsable;

    @Transient
    private Long commercialResponsableId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Transient
    private Long clientId;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification = LocalDateTime.now();

    public CrmEntreprise() {}

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
            this.statut = StatutEntreprise.PROSPECT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getSecteurActivite() { return secteurActivite; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }

    public String getTailleEntreprise() { return tailleEntreprise; }
    public void setTailleEntreprise(String tailleEntreprise) { this.tailleEntreprise = tailleEntreprise; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public StatutEntreprise getStatut() { return statut; }
    public void setStatut(StatutEntreprise statut) { this.statut = statut; }

    public User getCommercialResponsable() { return commercialResponsable; }
    public void setCommercialResponsable(User commercialResponsable) { this.commercialResponsable = commercialResponsable; }

    public Long getCommercialResponsableId() { return commercialResponsableId; }
    public void setCommercialResponsableId(Long commercialResponsableId) { this.commercialResponsableId = commercialResponsableId; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
