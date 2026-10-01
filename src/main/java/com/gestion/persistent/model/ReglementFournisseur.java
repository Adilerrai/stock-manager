package com.gestion.persistent.model;

import com.gestion.persistent.enums.ModePaiement;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reglements_fournisseur")
public class ReglementFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_reglement", unique = true, nullable = false)
    private String numeroReglement;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    @Column(name = "date_reglement", nullable = false)
    private LocalDateTime dateReglement = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_achat_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "lignes", "reglements"})
    private FactureAchat factureAchat;

    @Transient
    @com.fasterxml.jackson.annotation.JsonProperty("factureAchatId")
    private Long factureAchatId;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "notes")
    private String notes;

    @Column(name = "nom_banque")
    private String nomBanque;

    @Column(name = "numero_cheque")
    private String numeroCheque;

    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]")
    @Column(name = "date_echeance")
    private LocalDateTime dateEcheance;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    public ReglementFournisseur() {}

    @PrePersist
    @PreUpdate
    public void prePersist() {
        Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        if (tenant != null) {
            this.pointDeVenteId = tenant;
        } else if (this.pointDeVenteId == null) {
            this.pointDeVenteId = 1L;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroReglement() { return numeroReglement; }
    public void setNumeroReglement(String numeroReglement) { this.numeroReglement = numeroReglement; }

    public LocalDateTime getDateReglement() { return dateReglement; }
    public void setDateReglement(LocalDateTime dateReglement) { this.dateReglement = dateReglement; }

    public FactureAchat getFactureAchat() { return factureAchat; }
    public void setFactureAchat(FactureAchat factureAchat) { 
        this.factureAchat = factureAchat;
        if (factureAchat != null && factureAchat.getId() != null) {
            this.factureAchatId = factureAchat.getId();
        }
    }

    public Long getFactureAchatId() {
        if (factureAchatId != null) return factureAchatId;
        if (factureAchat != null) return factureAchat.getId();
        return null;
    }

    public void setFactureAchatId(Long factureAchatId) {
        this.factureAchatId = factureAchatId;
    }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    @com.fasterxml.jackson.annotation.JsonProperty("modeReglement")
    public void setModeReglement(ModePaiement modeReglement) {
        if (this.modePaiement == null) {
            this.modePaiement = modeReglement;
        }
    }

    public ModePaiement getModeReglement() {
        return this.modePaiement;
    }

    public String getReferencePaiement() { return referencePaiement; }
    public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getNomBanque() { return nomBanque; }
    public void setNomBanque(String nomBanque) { this.nomBanque = nomBanque; }

    public String getNumeroCheque() { return numeroCheque; }
    public void setNumeroCheque(String numeroCheque) { this.numeroCheque = numeroCheque; }

    public LocalDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDateTime dateEcheance) { this.dateEcheance = dateEcheance; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}

