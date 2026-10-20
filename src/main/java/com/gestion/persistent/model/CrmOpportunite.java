package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.EtapeOpportunite;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_opportunites")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmOpportunite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(nullable = false, length = 200)
    private String titre;

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
    @JoinColumn(name = "commercial_id")
    private User commercial;

    @Transient
    private Long commercialId;

    @Column(name = "montant_estime", precision = 15, scale = 2)
    private BigDecimal montantEstime = BigDecimal.ZERO;

    private Integer probabilite = 10;

    @Column(name = "date_cloture_prevue")
    private LocalDate dateCloturePrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EtapeOpportunite etape = EtapeOpportunite.NOUVEAU;

    @Column(name = "motif_perte", columnDefinition = "TEXT")
    private String motifPerte;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "devis_id")
    private Devis devis;

    @Transient
    private Long devisId;

    @Column(name = "date_cloture_effective")
    private LocalDateTime dateClotureEffective;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification = LocalDateTime.now();

    public CrmOpportunite() {}

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
        if (this.montantEstime == null) {
            this.montantEstime = BigDecimal.ZERO;
        }
        if (this.probabilite == null) {
            this.probabilite = 10;
        }
        if (this.etape == null) {
            this.etape = EtapeOpportunite.NOUVEAU;
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

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public CrmEntreprise getEntreprise() { return entreprise; }
    public void setEntreprise(CrmEntreprise entreprise) { this.entreprise = entreprise; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public CrmContact getContact() { return contact; }
    public void setContact(CrmContact contact) { this.contact = contact; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public User getCommercial() { return commercial; }
    public void setCommercial(User commercial) { this.commercial = commercial; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public BigDecimal getMontantEstime() { return montantEstime; }
    public void setMontantEstime(BigDecimal montantEstime) { this.montantEstime = montantEstime; }

    public Integer getProbabilite() { return probabilite; }
    public void setProbabilite(Integer probabilite) { this.probabilite = probabilite; }

    public LocalDate getDateCloturePrevue() { return dateCloturePrevue; }
    public void setDateCloturePrevue(LocalDate dateCloturePrevue) { this.dateCloturePrevue = dateCloturePrevue; }

    public EtapeOpportunite getEtape() { return etape; }
    public void setEtape(EtapeOpportunite etape) { this.etape = etape; }

    public String getMotifPerte() { return motifPerte; }
    public void setMotifPerte(String motifPerte) { this.motifPerte = motifPerte; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Devis getDevis() { return devis; }
    public void setDevis(Devis devis) { this.devis = devis; }

    public Long getDevisId() { return devisId; }
    public void setDevisId(Long devisId) { this.devisId = devisId; }

    public LocalDateTime getDateClotureEffective() { return dateClotureEffective; }
    public void setDateClotureEffective(LocalDateTime dateClotureEffective) { this.dateClotureEffective = dateClotureEffective; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
