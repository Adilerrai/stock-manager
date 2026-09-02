package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutLivraison;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bons_livraison_client")
public class BonLivraisonClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_bl", unique = true, nullable = false)
    private String numeroBl;

    @Column(name = "date_bl", nullable = false)
    private LocalDateTime dateBl = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_client_id")
    private CommandeClient commandeClient;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private StatutLivraison statut = StatutLivraison.EN_ATTENTE;

    @Column(name = "montant_total", precision = 15, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "observations")
    private String observations;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "bonLivraisonClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneBonLivraisonClient> lignes = new ArrayList<>();

    public BonLivraisonClient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public LocalDateTime getDateBl() { return dateBl; }
    public void setDateBl(LocalDateTime dateBl) { this.dateBl = dateBl; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public CommandeClient getCommandeClient() { return commandeClient; }
    public void setCommandeClient(CommandeClient commandeClient) { this.commandeClient = commandeClient; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Facture facture;

    public List<LigneBonLivraisonClient> getLignes() { return lignes; }
    public void setLignes(List<LigneBonLivraisonClient> lignes) { this.lignes = lignes; }

    public Facture getFacture() { return facture; }
    public void setFacture(Facture facture) { this.facture = facture; }
    public Boolean isFacture() { return facture != null; }
}

