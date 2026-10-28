package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutReservationClient;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations_client", uniqueConstraints = {
    @UniqueConstraint(name = "uk_reservations_client_tenant_numero", columnNames = {"point_de_vente_id", "numero_reservation"})
})
public class ReservationClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_reservation", nullable = false, length = 60)
    private String numeroReservation;

    @Column(name = "date_reservation", nullable = false)
    private LocalDateTime dateReservation = LocalDateTime.now();

    @Column(name = "date_livraison_souhaitee")
    private LocalDate dateLivraisonSouhaitee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 40)
    private StatutReservationClient statut = StatutReservationClient.SOUMISE;

    @Column(name = "observations_client", columnDefinition = "TEXT")
    private String observationsClient;

    @Column(name = "notes_tenant", columnDefinition = "TEXT")
    private String notesTenant;

    @Column(name = "devis_genere_id")
    private Long devisGenereId;

    @Column(name = "commande_generee_id")
    private Long commandeGenereeId;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_modification")
    private LocalDateTime dateModification = LocalDateTime.now();

    @OneToMany(mappedBy = "reservationClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneReservationClient> lignes = new ArrayList<>();

    public ReservationClient() {}

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = tenant != null ? tenant : 1L;
        }
        if (this.dateReservation == null) {
            this.dateReservation = LocalDateTime.now();
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
        this.dateModification = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.dateModification = LocalDateTime.now();
    }

    public void addLigne(LigneReservationClient ligne) {
        this.lignes.add(ligne);
        ligne.setReservationClient(this);
    }

    public void removeLigne(LigneReservationClient ligne) {
        this.lignes.remove(ligne);
        ligne.setReservationClient(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroReservation() { return numeroReservation; }
    public void setNumeroReservation(String numeroReservation) { this.numeroReservation = numeroReservation; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public LocalDate getDateLivraisonSouhaitee() { return dateLivraisonSouhaitee; }
    public void setDateLivraisonSouhaitee(LocalDate dateLivraisonSouhaitee) { this.dateLivraisonSouhaitee = dateLivraisonSouhaitee; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public User getCreePar() { return creePar; }
    public void setCreePar(User creePar) { this.creePar = creePar; }

    public StatutReservationClient getStatut() { return statut; }
    public void setStatut(StatutReservationClient statut) { this.statut = statut; }

    public String getObservationsClient() { return observationsClient; }
    public void setObservationsClient(String observationsClient) { this.observationsClient = observationsClient; }

    public String getNotesTenant() { return notesTenant; }
    public void setNotesTenant(String notesTenant) { this.notesTenant = notesTenant; }

    public Long getDevisGenereId() { return devisGenereId; }
    public void setDevisGenereId(Long devisGenereId) { this.devisGenereId = devisGenereId; }

    public Long getCommandeGenereeId() { return commandeGenereeId; }
    public void setCommandeGenereeId(Long commandeGenereeId) { this.commandeGenereeId = commandeGenereeId; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public List<LigneReservationClient> getLignes() { return lignes; }
    public void setLignes(List<LigneReservationClient> lignes) { this.lignes = lignes; }
}
