package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.StatutCommandeClient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes_client")
public class CommandeClient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroCommande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;


    @Column(name = "client_nom")
    private String clientNom;

    @Column(name = "client_telephone")
    private String clientTelephone;

    @Column(name = "client_email")
    private String clientEmail;

    @Column(name = "adresse_livraison")
    private String adresseLivraison;

    @Enumerated(EnumType.STRING)
    private StatutCommandeClient statut = StatutCommandeClient.BROUILLON;

    @Column(name = "date_commande")
    private LocalDateTime dateCommande = LocalDateTime.now();

    @Column(name = "date_livraison_prevue")
    private LocalDateTime dateLivraisonPrevue;

    @Column(name = "montant_ht", precision = 10, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 10, scale = 2)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = BigDecimal.valueOf(20);

    private String observations;

    @OneToMany(mappedBy = "commandeClient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LigneCommandeClient> lignesCommande = new ArrayList<>();

    // Constructors
    public CommandeClient() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getAdresseLivraison() { return adresseLivraison; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }

    public StatutCommandeClient getStatut() { return statut; }
    public void setStatut(StatutCommandeClient statut) { this.statut = statut; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<LigneCommandeClient> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeClient> lignesCommande) { this.lignesCommande = lignesCommande; }
}