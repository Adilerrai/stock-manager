package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.gestion.persistent.enums.StatutTransfert;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transferts_stock")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TransfertStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_transfert", nullable = false, length = 50)
    private String numeroTransfert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_source_id", nullable = false)
    private Depot depotSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_destination_id", nullable = false)
    private Depot depotDestination;

    @Column(name = "date_transfert", nullable = false)
    private LocalDate dateTransfert = LocalDate.now();

    @Column(name = "date_expedition")
    private LocalDateTime dateExpedition;

    @Column(name = "date_reception")
    private LocalDate dateReception;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTransfert statut = StatutTransfert.BROUILLON;

    private String motif;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_user_id")
    private User validePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expedie_par_user_id")
    private User expediePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recu_par_user_id")
    private User recuPar;

    @OneToMany(mappedBy = "transfert", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneTransfertStock> lignes = new ArrayList<>();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public TransfertStock() {}

    public void addLigne(LigneTransfertStock ligne) {
        if (this.lignes == null) {
            this.lignes = new ArrayList<>();
        }
        this.lignes.add(ligne);
        ligne.setTransfert(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroTransfert() { return numeroTransfert; }
    public void setNumeroTransfert(String numeroTransfert) { this.numeroTransfert = numeroTransfert; }

    public Depot getDepotSource() { return depotSource; }
    public void setDepotSource(Depot depotSource) { this.depotSource = depotSource; }

    public Depot getDepotDestination() { return depotDestination; }
    public void setDepotDestination(Depot depotDestination) { this.depotDestination = depotDestination; }

    public LocalDate getDateTransfert() { return dateTransfert; }
    public void setDateTransfert(LocalDate dateTransfert) { this.dateTransfert = dateTransfert; }

    public LocalDateTime getDateExpedition() { return dateExpedition; }
    public void setDateExpedition(LocalDateTime dateExpedition) { this.dateExpedition = dateExpedition; }

    public LocalDate getDateReception() { return dateReception; }
    public void setDateReception(LocalDate dateReception) { this.dateReception = dateReception; }

    public StatutTransfert getStatut() { return statut; }
    public void setStatut(StatutTransfert statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getCreePar() { return creePar; }
    public void setCreePar(User creePar) { this.creePar = creePar; }

    public User getValidePar() { return validePar; }
    public void setValidePar(User validePar) { this.validePar = validePar; }

    public User getExpediePar() { return expediePar; }
    public void setExpediePar(User expediePar) { this.expediePar = expediePar; }

    public User getRecuPar() { return recuPar; }
    public void setRecuPar(User recuPar) { this.recuPar = recuPar; }

    public List<LigneTransfertStock> getLignes() { return lignes; }
    public void setLignes(List<LigneTransfertStock> lignes) { this.lignes = lignes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
