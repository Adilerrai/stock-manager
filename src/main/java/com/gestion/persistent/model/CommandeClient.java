package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutCommandeClient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes_client", uniqueConstraints = {
    @UniqueConstraint(name = "uk_commandes_client_tenant_numero", columnNames = {"point_de_vente_id", "numero_commande"})
})
public class CommandeClient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_commande", nullable = false)
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

    @Column(name = "remise_globale_pourcentage", precision = 5, scale = 2)
    private BigDecimal remiseGlobalePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_globale_montant", precision = 12, scale = 2)
    private BigDecimal remiseGlobaleMontant = BigDecimal.ZERO;

    @Column(name = "is_recurrente")
    private Boolean isRecurrente = false;

    @Column(name = "frequence_recurrence", length = 30)
    private String frequenceRecurrence; // HEBDOMADAIRE, MENSUEL, TRIMESTRIEL

    @Column(name = "prochaine_date_recurrence")
    private LocalDateTime prochaineDateRecurrence;

    private String observations;

    @OneToMany(mappedBy = "commandeClient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LigneCommandeClient> lignesCommande = new ArrayList<>();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            if (tenant != null) {
                this.pointDeVenteId = tenant;
            } else {
                this.pointDeVenteId = 1L;
            }
        }
        recalculerMontants();
    }

    public void recalculerMontants() {
        BigDecimal totalLignes = BigDecimal.ZERO;
        if (this.lignesCommande != null) {
            for (LigneCommandeClient ligne : this.lignesCommande) {
                if (Boolean.TRUE.equals(ligne.getAnnulee())) {
                    continue;
                }
                ligne.calculerMontantLigne();
                if (ligne.getMontantLigne() != null) {
                    totalLignes = totalLignes.add(ligne.getMontantLigne());
                }
            }
        }

        // Remise globale
        BigDecimal baseHT = totalLignes;
        if (this.remiseGlobalePourcentage != null && this.remiseGlobalePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reduction = baseHT.multiply(this.remiseGlobalePourcentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            baseHT = baseHT.subtract(reduction);
        }
        if (this.remiseGlobaleMontant != null && this.remiseGlobaleMontant.compareTo(BigDecimal.ZERO) > 0) {
            baseHT = baseHT.subtract(this.remiseGlobaleMontant);
        }

        this.montantHT = baseHT.compareTo(BigDecimal.ZERO) >= 0 ? baseHT : BigDecimal.ZERO;
        BigDecimal tva = this.tauxTVA != null ? this.tauxTVA : BigDecimal.valueOf(20);
        BigDecimal facteurTva = BigDecimal.ONE.add(tva.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP));
        this.montantTTC = this.montantHT.multiply(facteurTva).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    // Constructors
    public CommandeClient() {}

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

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

    public BigDecimal getRemiseGlobalePourcentage() { return remiseGlobalePourcentage != null ? remiseGlobalePourcentage : BigDecimal.ZERO; }
    public void setRemiseGlobalePourcentage(BigDecimal remiseGlobalePourcentage) { this.remiseGlobalePourcentage = remiseGlobalePourcentage; }

    public BigDecimal getRemiseGlobaleMontant() { return remiseGlobaleMontant != null ? remiseGlobaleMontant : BigDecimal.ZERO; }
    public void setRemiseGlobaleMontant(BigDecimal remiseGlobaleMontant) { this.remiseGlobaleMontant = remiseGlobaleMontant; }

    public Boolean getIsRecurrente() { return isRecurrente != null ? isRecurrente : false; }
    public void setIsRecurrente(Boolean isRecurrente) { this.isRecurrente = isRecurrente; }

    public String getFrequenceRecurrence() { return frequenceRecurrence; }
    public void setFrequenceRecurrence(String frequenceRecurrence) { this.frequenceRecurrence = frequenceRecurrence; }

    public LocalDateTime getProchaineDateRecurrence() { return prochaineDateRecurrence; }
    public void setProchaineDateRecurrence(LocalDateTime prochaineDateRecurrence) { this.prochaineDateRecurrence = prochaineDateRecurrence; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<LigneCommandeClient> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeClient> lignesCommande) { this.lignesCommande = lignesCommande; }
}
