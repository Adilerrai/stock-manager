package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_prix_produit", indexes = {
    @Index(name = "idx_hist_prix_produit", columnList = "produit_id"),
    @Index(name = "idx_hist_prix_tenant", columnList = "point_de_vente_id")
})
public class HistoriquePrixProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(name = "ancien_prix_vente", precision = 12, scale = 2)
    private BigDecimal ancienPrixVente;

    @Column(name = "nouveau_prix_vente", precision = 12, scale = 2)
    private BigDecimal nouveauPrixVente;

    @Column(name = "ancien_prix_min", precision = 12, scale = 2)
    private BigDecimal ancienPrixMin;

    @Column(name = "nouveau_prix_min", precision = 12, scale = 2)
    private BigDecimal nouveauPrixMin;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @Column(name = "modifie_par_id")
    private Long modifieParId;

    @Column(name = "modifie_par_nom")
    private String modifieParNom;

    @Column(name = "motif", length = 255)
    private String motif;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = tenant != null ? tenant : 1L;
        }
        if (this.dateModification == null) {
            this.dateModification = LocalDateTime.now();
        }
    }

    public HistoriquePrixProduit() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public BigDecimal getAncienPrixVente() { return ancienPrixVente; }
    public void setAncienPrixVente(BigDecimal ancienPrixVente) { this.ancienPrixVente = ancienPrixVente; }

    public BigDecimal getNouveauPrixVente() { return nouveauPrixVente; }
    public void setNouveauPrixVente(BigDecimal nouveauPrixVente) { this.nouveauPrixVente = nouveauPrixVente; }

    public BigDecimal getAncienPrixMin() { return ancienPrixMin; }
    public void setAncienPrixMin(BigDecimal ancienPrixMin) { this.ancienPrixMin = ancienPrixMin; }

    public BigDecimal getNouveauPrixMin() { return nouveauPrixMin; }
    public void setNouveauPrixMin(BigDecimal nouveauPrixMin) { this.nouveauPrixMin = nouveauPrixMin; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public Long getModifieParId() { return modifieParId; }
    public void setModifieParId(Long modifieParId) { this.modifieParId = modifieParId; }

    public String getModifieParNom() { return modifieParNom; }
    public void setModifieParNom(String modifieParNom) { this.modifieParNom = modifieParNom; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
