package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_offre_fournisseur")
public class LigneOffreFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offre_fournisseur_id", nullable = false)
    private OffreFournisseur offreFournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_demande_prix_id", nullable = false)
    private LigneDemandePrix ligneDemandePrix;

    @Column(name = "prix_unitaire_ht", nullable = false, precision = 12, scale = 2)
    private BigDecimal prixUnitaireHT = BigDecimal.ZERO;

    @Column(name = "prix_unitaire_devise", precision = 14, scale = 4)
    private BigDecimal prixUnitaireDevise;

    @Column(name = "code_sh", length = 30)
    private String codeSh;

    @Column(name = "remise_pourcentage", precision = 5, scale = 2)
    private BigDecimal remisePourcentage = BigDecimal.ZERO;

    @Column(name = "taux_tva", precision = 5, scale = 2)
    private BigDecimal tauxTVA = new BigDecimal("20.00");

    @Column(name = "quantite_proposee", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantiteProposee = BigDecimal.ONE;

    @Column(name = "disponible_stock")
    private Boolean disponibleStock = true;

    @Column(name = "est_retenue")
    private Boolean estRetenue = false;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    public LigneOffreFournisseur() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OffreFournisseur getOffreFournisseur() { return offreFournisseur; }
    public void setOffreFournisseur(OffreFournisseur offreFournisseur) { this.offreFournisseur = offreFournisseur; }

    public LigneDemandePrix getLigneDemandePrix() { return ligneDemandePrix; }
    public void setLigneDemandePrix(LigneDemandePrix ligneDemandePrix) { this.ligneDemandePrix = ligneDemandePrix; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getRemisePourcentage() { return remisePourcentage; }
    public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getQuantiteProposee() { return quantiteProposee; }
    public void setQuantiteProposee(BigDecimal quantiteProposee) { this.quantiteProposee = quantiteProposee; }

    public Boolean getDisponibleStock() { return disponibleStock; }
    public void setDisponibleStock(Boolean disponibleStock) { this.disponibleStock = disponibleStock; }

    public Boolean getEstRetenue() { return estRetenue; }
    public void setEstRetenue(Boolean estRetenue) { this.estRetenue = estRetenue; }

    public BigDecimal getPrixUnitaireDevise() { return prixUnitaireDevise; }
    public void setPrixUnitaireDevise(BigDecimal prixUnitaireDevise) { this.prixUnitaireDevise = prixUnitaireDevise; }

    public String getCodeSh() { return codeSh; }
    public void setCodeSh(String codeSh) { this.codeSh = codeSh; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public BigDecimal getMontantTotalHT() {
        BigDecimal qte = quantiteProposee != null ? quantiteProposee : BigDecimal.ZERO;
        BigDecimal pu = prixUnitaireHT != null ? prixUnitaireHT : BigDecimal.ZERO;
        BigDecimal total = pu.multiply(qte);
        if (remisePourcentage != null && remisePourcentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal facteurRemise = BigDecimal.ONE.subtract(remisePourcentage.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
            total = total.multiply(facteurRemise);
        }
        return total.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
