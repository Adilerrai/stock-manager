package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutRapportVisite;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_rapports_visite")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CrmRapportVisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visite_id", nullable = false, unique = true)
    private CrmVisite visite;

    @Transient
    private Long visiteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutRapportVisite statut = StatutRapportVisite.BROUILLON;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resume;

    @Column(name = "interlocuteurs_presents", columnDefinition = "TEXT")
    private String interlocuteursPresents;

    @Column(name = "besoins_identifies", columnDefinition = "TEXT")
    private String besoinsIdentifies;

    @Column(name = "objections_soulevees", columnDefinition = "TEXT")
    private String objectionsSoulevees;

    @Column(length = 100)
    private String resultat;

    @Column(name = "prochaine_etape", columnDefinition = "TEXT")
    private String prochaineEtape;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes = 30;

    @Column(name = "soumis_le")
    private LocalDateTime soumisLe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_user_id")
    private User validePar;

    @Transient
    private Long valideParUserId;

    @Column(name = "valide_le")
    private LocalDateTime valideLe;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification = LocalDateTime.now();

    public CrmRapportVisite() {}

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
            this.statut = StatutRapportVisite.BROUILLON;
        }
        if (this.dureeMinutes == null) {
            this.dureeMinutes = 30;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    public boolean isVerrouille() {
        return this.statut == StatutRapportVisite.VALIDE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public CrmVisite getVisite() { return visite; }
    public void setVisite(CrmVisite visite) { this.visite = visite; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public StatutRapportVisite getStatut() { return statut; }
    public void setStatut(StatutRapportVisite statut) { this.statut = statut; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }

    public String getInterlocuteursPresents() { return interlocuteursPresents; }
    public void setInterlocuteursPresents(String interlocuteursPresents) { this.interlocuteursPresents = interlocuteursPresents; }

    public String getBesoinsIdentifies() { return besoinsIdentifies; }
    public void setBesoinsIdentifies(String besoinsIdentifies) { this.besoinsIdentifies = besoinsIdentifies; }

    public String getObjectionsSoulevees() { return objectionsSoulevees; }
    public void setObjectionsSoulevees(String objectionsSoulevees) { this.objectionsSoulevees = objectionsSoulevees; }

    public String getResultat() { return resultat; }
    public void setResultat(String resultat) { this.resultat = resultat; }

    public String getProchaineEtape() { return prochaineEtape; }
    public void setProchaineEtape(String prochaineEtape) { this.prochaineEtape = prochaineEtape; }

    public Integer getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public LocalDateTime getSoumisLe() { return soumisLe; }
    public void setSoumisLe(LocalDateTime soumisLe) { this.soumisLe = soumisLe; }

    public User getValidePar() { return validePar; }
    public void setValidePar(User validePar) { this.validePar = validePar; }

    public Long getValideParUserId() { return valideParUserId; }
    public void setValideParUserId(Long valideParUserId) { this.valideParUserId = valideParUserId; }

    public LocalDateTime getValideLe() { return valideLe; }
    public void setValideLe(LocalDateTime valideLe) { this.valideLe = valideLe; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
