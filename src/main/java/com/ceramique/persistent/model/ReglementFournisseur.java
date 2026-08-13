package com.ceramique.persistent.model;

import com.ceramique.persistent.enums.ModePaiement;
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

    @Column(name = "date_reglement", nullable = false)
    private LocalDateTime dateReglement = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_achat_id", nullable = false)
    private FactureAchat factureAchat;

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", nullable = false)
    private ModePaiement modePaiement;

    @Column(name = "reference_paiement")
    private String referencePaiement;

    @Column(name = "notes")
    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    public ReglementFournisseur() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroReglement() { return numeroReglement; }
    public void setNumeroReglement(String numeroReglement) { this.numeroReglement = numeroReglement; }

    public LocalDateTime getDateReglement() { return dateReglement; }
    public void setDateReglement(LocalDateTime dateReglement) { this.dateReglement = dateReglement; }

    public FactureAchat getFactureAchat() { return factureAchat; }
    public void setFactureAchat(FactureAchat factureAchat) { this.factureAchat = factureAchat; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public String getReferencePaiement() { return referencePaiement; }
    public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
