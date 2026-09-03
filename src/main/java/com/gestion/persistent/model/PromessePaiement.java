package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutPromesse;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "promesses_paiement")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PromessePaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    private Facture facture;

    @Column(name = "date_promesse", nullable = false)
    private LocalDateTime datePromesse = LocalDateTime.now();

    @Column(name = "date_echeance_promise", nullable = false)
    private LocalDate dateEcheancePromise;

    @Column(name = "montant_promis", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantPromis = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPromesse statut = StatutPromesse.EN_ATTENTE;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enregistre_par_user_id")
    private User enregistrePar;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    public PromessePaiement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Facture getFacture() { return facture; }
    public void setFacture(Facture facture) { this.facture = facture; }

    public LocalDateTime getDatePromesse() { return datePromesse; }
    public void setDatePromesse(LocalDateTime datePromesse) { this.datePromesse = datePromesse; }

    public LocalDate getDateEcheancePromise() { return dateEcheancePromise; }
    public void setDateEcheancePromise(LocalDate dateEcheancePromise) { this.dateEcheancePromise = dateEcheancePromise; }

    public BigDecimal getMontantPromis() { return montantPromis; }
    public void setMontantPromis(BigDecimal montantPromis) { this.montantPromis = montantPromis; }

    public StatutPromesse getStatut() { return statut; }
    public void setStatut(StatutPromesse statut) { this.statut = statut; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getEnregistrePar() { return enregistrePar; }
    public void setEnregistrePar(User enregistrePar) { this.enregistrePar = enregistrePar; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
