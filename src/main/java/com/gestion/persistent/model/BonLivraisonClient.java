package com.gestion.persistent.model;

import com.gestion.persistent.enums.StatutLivraison;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bons_livraison_client", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bl_client_tenant_numero", columnNames = {"point_de_vente_id", "numero_bl"})
})
public class BonLivraisonClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_bl", nullable = false)
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

    @Column(name = "remise_globale_pourcentage", precision = 5, scale = 2)
    private BigDecimal remiseGlobalePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_globale_montant", precision = 12, scale = 2)
    private BigDecimal remiseGlobaleMontant = BigDecimal.ZERO;

    @Column(name = "montant_total", precision = 15, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "observations")
    private String observations;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "bonLivraisonClient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneBonLivraisonClient> lignes = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            this.pointDeVenteId = tenant != null ? tenant : 1L;
        }
        recalculerMontantTotal();
    }

    public void recalculerMontantTotal() {
        BigDecimal totalLignes = BigDecimal.ZERO;
        if (this.lignes != null) {
            for (LigneBonLivraisonClient ligne : this.lignes) {
                ligne.calculerMontantLigne();
                if (ligne.getMontantLigne() != null) {
                    totalLignes = totalLignes.add(ligne.getMontantLigne());
                }
            }
        }

        BigDecimal netTotal = totalLignes;
        if (this.remiseGlobalePourcentage != null && this.remiseGlobalePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reduction = netTotal.multiply(this.remiseGlobalePourcentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            netTotal = netTotal.subtract(reduction);
        }
        if (this.remiseGlobaleMontant != null && this.remiseGlobaleMontant.compareTo(BigDecimal.ZERO) > 0) {
            netTotal = netTotal.subtract(this.remiseGlobaleMontant);
        }

        this.montantTotal = netTotal.compareTo(BigDecimal.ZERO) >= 0 ? netTotal : BigDecimal.ZERO;
    }

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

    public BigDecimal getRemiseGlobalePourcentage() { return remiseGlobalePourcentage != null ? remiseGlobalePourcentage : BigDecimal.ZERO; }
    public void setRemiseGlobalePourcentage(BigDecimal remiseGlobalePourcentage) { this.remiseGlobalePourcentage = remiseGlobalePourcentage; }

    public BigDecimal getRemiseGlobaleMontant() { return remiseGlobaleMontant != null ? remiseGlobaleMontant : BigDecimal.ZERO; }
    public void setRemiseGlobaleMontant(BigDecimal remiseGlobaleMontant) { this.remiseGlobaleMontant = remiseGlobaleMontant; }

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

    public void addLigne(LigneBonLivraisonClient ligne) {
        this.lignes.add(ligne);
        ligne.setBonLivraisonClient(this);
    }

    public Facture getFacture() { return facture; }
    public void setFacture(Facture facture) { this.facture = facture; }
    public Boolean isFacture() { return facture != null; }
}

