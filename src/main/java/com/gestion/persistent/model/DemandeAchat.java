package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.PrioriteDemandeAchat;
import com.gestion.persistent.enums.StatutDemandeAchat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "demandes_achat", uniqueConstraints = {
    @UniqueConstraint(name = "uk_da_tenant_numero", columnNames = {"point_de_vente_id", "numero_da"})
})
public class DemandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_da", nullable = false, length = 50)
    private String numeroDa;

    @Column(name = "demandeur_nom", length = 100)
    private String demandeurNom;

    @Column(name = "service_demandeur", length = 100)
    private String serviceDemandeur;

    @Column(nullable = false, length = 255)
    private String objet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private StatutDemandeAchat statut = StatutDemandeAchat.BROUILLON;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PrioriteDemandeAchat priorite = PrioriteDemandeAchat.NORMALE;

    @Column(name = "date_demande")
    private LocalDateTime dateDemande = LocalDateTime.now();

    @Column(name = "date_souhaitee")
    private LocalDateTime dateSouhaitee;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "demande_prix_id")
    private Long demandePrixId;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "demandeAchat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDemandeAchat> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
        if (this.dateDemande == null) {
            this.dateDemande = LocalDateTime.now();
        }
    }

    public DemandeAchat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroDa() { return numeroDa; }
    public void setNumeroDa(String numeroDa) { this.numeroDa = numeroDa; }

    public String getDemandeurNom() { return demandeurNom; }
    public void setDemandeurNom(String demandeurNom) { this.demandeurNom = demandeurNom; }

    public String getServiceDemandeur() { return serviceDemandeur; }
    public void setServiceDemandeur(String serviceDemandeur) { this.serviceDemandeur = serviceDemandeur; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public StatutDemandeAchat getStatut() { return statut; }
    public void setStatut(StatutDemandeAchat statut) { this.statut = statut; }

    public PrioriteDemandeAchat getPriorite() { return priorite; }
    public void setPriorite(PrioriteDemandeAchat priorite) { this.priorite = priorite; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public LocalDateTime getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDateTime dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getDemandePrixId() { return demandePrixId; }
    public void setDemandePrixId(Long demandePrixId) { this.demandePrixId = demandePrixId; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneDemandeAchat> getLignes() { return lignes; }
    public void setLignes(List<LigneDemandeAchat> lignes) { this.lignes = lignes; }
}
