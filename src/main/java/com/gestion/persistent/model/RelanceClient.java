package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.CanalRelance;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "relances_clients")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RelanceClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @Column(name = "date_relance", nullable = false)
    private LocalDateTime dateRelance = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalRelance canal = CanalRelance.TELEPHONE;

    private String interlocuteur;

    @Column(nullable = false, length = 1000)
    private String commentaire;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "effectue_par_user_id")
    private User effectuePar;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public RelanceClient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Facture getFacture() { return facture; }
    public void setFacture(Facture facture) { this.facture = facture; }

    public LocalDateTime getDateRelance() { return dateRelance; }
    public void setDateRelance(LocalDateTime dateRelance) { this.dateRelance = dateRelance; }

    public CanalRelance getCanal() { return canal; }
    public void setCanal(CanalRelance canal) { this.canal = canal; }

    public String getInterlocuteur() { return interlocuteur; }
    public void setInterlocuteur(String interlocuteur) { this.interlocuteur = interlocuteur; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public User getEffectuePar() { return effectuePar; }
    public void setEffectuePar(User effectuePar) { this.effectuePar = effectuePar; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
