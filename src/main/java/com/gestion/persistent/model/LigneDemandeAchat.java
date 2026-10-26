package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lignes_demande_achat")
public class LigneDemandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_achat_id", nullable = false)
    private DemandeAchat demandeAchat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    @Column(name = "reference", length = 100)
    private String reference;

    @Column(name = "designation", nullable = false, length = 255)
    private String designation;

    @Column(name = "quantite_demandee", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantiteDemandee = BigDecimal.ONE;

    @Column(name = "quantite_en_ddp", nullable = false, precision = 12, scale = 2)
    private BigDecimal quantiteEnDdp = BigDecimal.ZERO;

    @Column(name = "unite", length = 20)
    private String unite = "U";

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 30)
    private com.gestion.persistent.enums.StatutLigneDemandeAchat statut = com.gestion.persistent.enums.StatutLigneDemandeAchat.NON_TRAITEE;

    @Column(name = "remarques", length = 255)
    private String remarques;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    public LigneDemandeAchat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DemandeAchat getDemandeAchat() { return demandeAchat; }
    public void setDemandeAchat(DemandeAchat demandeAchat) { this.demandeAchat = demandeAchat; }

    public Produit getProduit() { return produit; }
    public void setProduit(Produit produit) { this.produit = produit; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public BigDecimal getQuantiteEnDdp() { return quantiteEnDdp; }
    public void setQuantiteEnDdp(BigDecimal quantiteEnDdp) { this.quantiteEnDdp = quantiteEnDdp; }

    public com.gestion.persistent.enums.StatutLigneDemandeAchat getStatut() { return statut; }
    public void setStatut(com.gestion.persistent.enums.StatutLigneDemandeAchat statut) { this.statut = statut; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
