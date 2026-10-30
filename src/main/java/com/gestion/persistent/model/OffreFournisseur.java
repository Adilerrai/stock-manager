package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.enums.StatutOffreFournisseur;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "offres_fournisseurs")
public class OffreFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_prix_id", nullable = false)
    private DemandePrix demandePrix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(name = "numero_devis", length = 100)
    private String numeroDevis;

    @Column(name = "date_reception")
    private LocalDateTime dateReception = LocalDateTime.now();

    @Column(name = "delai_livraison_jours")
    private Integer delaiLivraisonJours = 0;

    @Column(name = "frais_port", precision = 12, scale = 2)
    private BigDecimal fraisPort = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutOffreFournisseur statut = StatutOffreFournisseur.EN_ATTENTE;

    @Column(name = "conditions_paiement", length = 100)
    private String conditionsPaiement;

    @Column(length = 10)
    private String devise = "MAD";

    @Column(name = "taux_change_estime", precision = 12, scale = 6)
    private BigDecimal tauxChangeEstime = BigDecimal.ONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoterm", length = 10)
    private com.gestion.persistent.enums.Incoterm incoterm;

    @Column(name = "lieu_incoterm", length = 100)
    private String lieuIncoterm;

    @Column(name = "port_embarquement", length = 100)
    private String portEmbarquement;

    @Column(name = "validite_offre")
    private java.time.LocalDate validiteOffre;

    @Column(name = "code_douanier_sh", length = 30)
    private String codeDouanierSh;

    @Column(name = "acompte_exige_pct", precision = 5, scale = 2)
    private BigDecimal acompteExigePct = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "offreFournisseur", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneOffreFournisseur> lignes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    public OffreFournisseur() {}

    public BigDecimal calculateTotalHT() {
        BigDecimal total = BigDecimal.ZERO;
        if (lignes != null) {
            for (LigneOffreFournisseur l : lignes) {
                total = total.add(l.getMontantTotalHT());
            }
        }
        if (fraisPort != null) {
            total = total.add(fraisPort);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DemandePrix getDemandePrix() { return demandePrix; }
    public void setDemandePrix(DemandePrix demandePrix) { this.demandePrix = demandePrix; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; }

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }

    public Integer getDelaiLivraisonJours() { return delaiLivraisonJours; }
    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) { this.delaiLivraisonJours = delaiLivraisonJours; }

    public BigDecimal getFraisPort() { return fraisPort != null ? fraisPort : BigDecimal.ZERO; }
    public void setFraisPort(BigDecimal fraisPort) { this.fraisPort = fraisPort; }

    public StatutOffreFournisseur getStatut() { return statut; }
    public void setStatut(StatutOffreFournisseur statut) { this.statut = statut; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public BigDecimal getTauxChangeEstime() { return tauxChangeEstime; }
    public void setTauxChangeEstime(BigDecimal tauxChangeEstime) { this.tauxChangeEstime = tauxChangeEstime; }

    public com.gestion.persistent.enums.Incoterm getIncoterm() { return incoterm; }
    public void setIncoterm(com.gestion.persistent.enums.Incoterm incoterm) { this.incoterm = incoterm; }

    public String getLieuIncoterm() { return lieuIncoterm; }
    public void setLieuIncoterm(String lieuIncoterm) { this.lieuIncoterm = lieuIncoterm; }

    public String getPortEmbarquement() { return portEmbarquement; }
    public void setPortEmbarquement(String portEmbarquement) { this.portEmbarquement = portEmbarquement; }

    public java.time.LocalDate getValiditeOffre() { return validiteOffre; }
    public void setValiditeOffre(java.time.LocalDate validiteOffre) { this.validiteOffre = validiteOffre; }

    public String getCodeDouanierSh() { return codeDouanierSh; }
    public void setCodeDouanierSh(String codeDouanierSh) { this.codeDouanierSh = codeDouanierSh; }

    public BigDecimal getAcompteExigePct() { return acompteExigePct; }
    public void setAcompteExigePct(BigDecimal acompteExigePct) { this.acompteExigePct = acompteExigePct; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneOffreFournisseur> getLignes() { return lignes; }
    public void setLignes(List<LigneOffreFournisseur> lignes) { this.lignes = lignes; }
}
