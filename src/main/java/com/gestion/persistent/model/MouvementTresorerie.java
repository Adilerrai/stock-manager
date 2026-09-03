package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeMouvementTresorerie;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mouvements_tresorerie")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MouvementTresorerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false)
    private TypeMouvementTresorerie typeMouvement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_source_id", nullable = false)
    private CompteFinancier compteSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_destination_id")
    private CompteFinancier compteDestination;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();

    @Column(nullable = false)
    private String motif;

    @Column(name = "justificatif_reference")
    private String justificatifReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_user_id")
    private User effectuePar;

    @Column(name = "solde_apres_source", precision = 15, scale = 2)
    private BigDecimal soldeApresSource;

    @Column(name = "solde_apres_destination", precision = 15, scale = 2)
    private BigDecimal soldeApresDestination;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public MouvementTresorerie() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public TypeMouvementTresorerie getTypeMouvement() { return typeMouvement; }
    public void setTypeMouvement(TypeMouvementTresorerie typeMouvement) { this.typeMouvement = typeMouvement; }

    public CompteFinancier getCompteSource() { return compteSource; }
    public void setCompteSource(CompteFinancier compteSource) { this.compteSource = compteSource; }

    public CompteFinancier getCompteDestination() { return compteDestination; }
    public void setCompteDestination(CompteFinancier compteDestination) { this.compteDestination = compteDestination; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDateTime getDateMouvement() { return dateMouvement; }
    public void setDateMouvement(LocalDateTime dateMouvement) { this.dateMouvement = dateMouvement; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getJustificatifReference() { return justificatifReference; }
    public void setJustificatifReference(String justificatifReference) { this.justificatifReference = justificatifReference; }

    public User getEffectuePar() { return effectuePar; }
    public void setEffectuePar(User effectuePar) { this.effectuePar = effectuePar; }

    public BigDecimal getSoldeApresSource() { return soldeApresSource; }
    public void setSoldeApresSource(BigDecimal soldeApresSource) { this.soldeApresSource = soldeApresSource; }

    public BigDecimal getSoldeApresDestination() { return soldeApresDestination; }
    public void setSoldeApresDestination(BigDecimal soldeApresDestination) { this.soldeApresDestination = soldeApresDestination; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
