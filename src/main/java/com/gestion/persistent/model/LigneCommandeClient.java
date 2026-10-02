package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_commande_client")
public class LigneCommandeClient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_client_id", nullable = false)
    private CommandeClient commandeClient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantite;

    @Column(name = "quantite_commandee", precision = 12, scale = 3)
    private BigDecimal quantiteCommandee;

    @Column(name = "quantite_livree", precision = 12, scale = 3)
    private BigDecimal quantiteLivree = BigDecimal.ZERO;

    @Column(name = "quantite_reliquat", precision = 12, scale = 3)
    private BigDecimal quantiteReliquat;

    @Column(name = "prix_unitaire", precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "remise_pourcentage", precision = 5, scale = 2)
    private BigDecimal remisePourcentage = BigDecimal.ZERO;

    @Column(name = "remise_montant", precision = 12, scale = 2)
    private BigDecimal remiseMontant = BigDecimal.ZERO;

    @Column(name = "montant_ligne", precision = 10, scale = 2)
    private BigDecimal montantLigne;

    @Column(name = "annulee")
    private Boolean annulee = false;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    private String observations;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (this.quantiteCommandee == null) {
            this.quantiteCommandee = this.quantite != null ? this.quantite : BigDecimal.ZERO;
        }
        if (this.quantiteLivree == null) {
            this.quantiteLivree = BigDecimal.ZERO;
        }
        if (this.quantiteReliquat == null) {
            this.quantiteReliquat = calculerReliquat();
        }
        calculerMontantLigne();
    }

    public BigDecimal calculerReliquat() {
        BigDecimal cmd = this.quantiteCommandee != null ? this.quantiteCommandee : (this.quantite != null ? this.quantite : BigDecimal.ZERO);
        BigDecimal liv = this.quantiteLivree != null ? this.quantiteLivree : BigDecimal.ZERO;
        BigDecimal diff = cmd.subtract(liv);
        return diff.compareTo(BigDecimal.ZERO) > 0 ? diff : BigDecimal.ZERO;
    }

    public void calculerMontantLigne() {
        BigDecimal qte = this.quantite != null ? this.quantite : (this.quantiteCommandee != null ? this.quantiteCommandee : BigDecimal.ZERO);
        BigDecimal pu = this.prixUnitaire != null ? this.prixUnitaire : BigDecimal.ZERO;
        BigDecimal brut = qte.multiply(pu);

        // Appliquer remise pourcentage
        BigDecimal montantApresRemisePct = brut;
        if (this.remisePourcentage != null && this.remisePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal reduction = brut.multiply(this.remisePourcentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            montantApresRemisePct = brut.subtract(reduction);
        }

        // Appliquer remise montant fixe
        if (this.remiseMontant != null && this.remiseMontant.compareTo(BigDecimal.ZERO) > 0) {
            montantApresRemisePct = montantApresRemisePct.subtract(this.remiseMontant);
        }

        this.montantLigne = montantApresRemisePct.compareTo(BigDecimal.ZERO) >= 0 ? montantApresRemisePct : BigDecimal.ZERO;
    }

    // Constructors
    public LigneCommandeClient() {}

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CommandeClient getCommandeClient() { return commandeClient; }
    public void setCommandeClient(CommandeClient commandeClient) { this.commandeClient = commandeClient; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { 
        this.quantite = quantite;
        if (this.quantiteCommandee == null) {
            this.quantiteCommandee = quantite;
        }
    }

    public BigDecimal getQuantiteCommandee() { return quantiteCommandee != null ? quantiteCommandee : quantite; }
    public void setQuantiteCommandee(BigDecimal quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public BigDecimal getQuantiteLivree() { return quantiteLivree != null ? quantiteLivree : BigDecimal.ZERO; }
    public void setQuantiteLivree(BigDecimal quantiteLivree) { 
        this.quantiteLivree = quantiteLivree != null ? quantiteLivree : BigDecimal.ZERO;
        this.quantiteReliquat = calculerReliquat();
    }

    public BigDecimal getQuantiteReliquat() { return quantiteReliquat != null ? quantiteReliquat : calculerReliquat(); }
    public void setQuantiteReliquat(BigDecimal quantiteReliquat) { this.quantiteReliquat = quantiteReliquat; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage != null ? remisePourcentage : BigDecimal.ZERO; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getRemiseMontant() { return remiseMontant != null ? remiseMontant : BigDecimal.ZERO; }
    public void setRemiseMontant(BigDecimal remiseMontant) { this.remiseMontant = remiseMontant; }

    public BigDecimal getMontantLigne() { return montantLigne; }
    public void setMontantLigne(BigDecimal montantLigne) { this.montantLigne = montantLigne; }

    public Boolean getAnnulee() { return annulee != null ? annulee : false; }
    public void setAnnulee(Boolean annulee) { this.annulee = annulee; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }
}
