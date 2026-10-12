package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.StatutDemandePrix;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demandes_prix", uniqueConstraints = {
    @UniqueConstraint(name = "uk_ddp_tenant_numero", columnNames = {"point_de_vente_id", "numero_ddp"})
})
public class DemandePrix {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_ddp", nullable = false, length = 50)
    private String numeroDdp;

    @Column(nullable = false, length = 255)
    private String objet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutDemandePrix statut = StatutDemandePrix.BROUILLON;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_limite_reponse")
    private LocalDateTime dateLimiteReponse;

    @Column(name = "demande_achat_id", length = 50)
    private String demandeAchatId;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "demandePrix", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandePrix> lignes = new ArrayList<>();

    @OneToMany(mappedBy = "demandePrix", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OffreFournisseur> offres = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    public DemandePrix() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroDdp() { return numeroDdp; }
    public void setNumeroDdp(String numeroDdp) { this.numeroDdp = numeroDdp; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public StatutDemandePrix getStatut() { return statut; }
    public void setStatut(StatutDemandePrix statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateLimiteReponse() { return dateLimiteReponse; }
    public void setDateLimiteReponse(LocalDateTime dateLimiteReponse) { this.dateLimiteReponse = dateLimiteReponse; }

    public String getDemandeAchatId() { return demandeAchatId; }
    public void setDemandeAchatId(String demandeAchatId) { this.demandeAchatId = demandeAchatId; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneDemandePrix> getLignes() { return lignes; }
    public void setLignes(List<LigneDemandePrix> lignes) { this.lignes = lignes; }

    public List<OffreFournisseur> getOffres() { return offres; }
    public void setOffres(List<OffreFournisseur> offres) { this.offres = offres; }
}
