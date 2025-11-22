package com.ceramique.persistent.model;

import com.acommon.persistant.model.PointDeVente;
import com.ceramique.persistent.enums.StatutLivraison;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "livraisons")
public class Livraison {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroLivraison;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = true)
    private Commande commande;

    @Column(name = "date_livraison")
    private LocalDateTime dateLivraison = LocalDateTime.now();

    private String transporteur;

    private BigDecimal montantTotal;

    private String observations;

    @OneToMany(mappedBy = "livraison", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LigneLivraison> lignesLivraison = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutLivraison statut = StatutLivraison.EN_ATTENTE;

    @Column(name = "numero_suivi")
    private String numeroSuivi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_de_vente_id", nullable = false)
    private PointDeVente pointDeVente;

    // Constructors, getters, setters
    public Livraison() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroLivraison() { return numeroLivraison; }
    public void setNumeroLivraison(String numeroLivraison) { this.numeroLivraison = numeroLivraison; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public LocalDateTime getDateLivraison() { return dateLivraison; }
    public void setDateLivraison(LocalDateTime dateLivraison) { this.dateLivraison = dateLivraison; }

    public String getTransporteur() { return transporteur; }
    public void setTransporteur(String transporteur) { this.transporteur = transporteur; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<LigneLivraison> getLignesLivraison() { return lignesLivraison; }
    public void setLignesLivraison(List<LigneLivraison> lignesLivraison) { this.lignesLivraison = lignesLivraison; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public String getNumeroSuivi() { return numeroSuivi; }
    public void setNumeroSuivi(String numeroSuivi) { this.numeroSuivi = numeroSuivi; }

    public PointDeVente getPointDeVente() { return pointDeVente; }
    public void setPointDeVente(PointDeVente pointDeVente) { this.pointDeVente = pointDeVente; }
}
