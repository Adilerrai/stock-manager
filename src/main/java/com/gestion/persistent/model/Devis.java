package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutDevis;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "devis")
public class Devis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroDevis;

    @Column(name = "date_devis", nullable = false)
    private LocalDate dateDevis = LocalDate.now();

    @Column(name = "date_validite")
    private LocalDate dateValidite = LocalDate.now().plusDays(30);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @OneToMany(mappedBy = "devis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneDevis> lignes = new ArrayList<>();

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Column(name = "remise_globale", precision = 15, scale = 2)
    private BigDecimal remiseGlobale = BigDecimal.ZERO;

    @Column(name = "montant_final", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantFinal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDevis statut = StatutDevis.BROUILLON;

    private String notes;

    @Column(name = "conditions_paiement")
    private String conditionsPaiement;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @Column(name = "commande_generee_id")
    private Long commandeGenereeId;

    @Column(name = "facture_generee_id")
    private Long factureGenereeId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Devis() {
    }

    public void calculerTotaux() {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        BigDecimal totalTTC = BigDecimal.ZERO;

        if (lignes != null) {
            for (LigneDevis ligne : lignes) {
                ligne.setDevis(this);
                ligne.calculerMontants();
                totalHT = totalHT.add(ligne.getMontantHT());
                totalTVA = totalTVA.add(ligne.getMontantTVA());
                totalTTC = totalTTC.add(ligne.getMontantTTC());
            }
        }

        this.montantHT = totalHT;
        this.montantTVA = totalTVA;
        this.montantTTC = totalTTC;

        if (remiseGlobale == null) remiseGlobale = BigDecimal.ZERO;
        this.montantFinal = this.montantTTC.subtract(remiseGlobale).max(BigDecimal.ZERO);
    }

    public void addLigne(LigneDevis ligne) {
        lignes.add(ligne);
        ligne.setDevis(this);
    }

    public void removeLigne(LigneDevis ligne) {
        lignes.remove(ligne);
        ligne.setDevis(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroDevis() {
        return numeroDevis;
    }

    public void setNumeroDevis(String numeroDevis) {
        this.numeroDevis = numeroDevis;
    }

    public LocalDate getDateDevis() {
        return dateDevis;
    }

    public void setDateDevis(LocalDate dateDevis) {
        this.dateDevis = dateDevis;
    }

    public LocalDate getDateValidite() {
        return dateValidite;
    }

    public void setDateValidite(LocalDate dateValidite) {
        this.dateValidite = dateValidite;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public User getCreePar() {
        return creePar;
    }

    public void setCreePar(User creePar) {
        this.creePar = creePar;
    }

    public List<LigneDevis> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneDevis> lignes) {
        this.lignes = lignes;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTVA() {
        return montantTVA;
    }

    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public BigDecimal getRemiseGlobale() {
        return remiseGlobale;
    }

    public void setRemiseGlobale(BigDecimal remiseGlobale) {
        this.remiseGlobale = remiseGlobale;
    }

    public BigDecimal getMontantFinal() {
        return montantFinal;
    }

    public void setMontantFinal(BigDecimal montantFinal) {
        this.montantFinal = montantFinal;
    }

    public StatutDevis getStatut() {
        return statut;
    }

    public void setStatut(StatutDevis statut) {
        this.statut = statut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getConditionsPaiement() {
        return conditionsPaiement;
    }

    public void setConditionsPaiement(String conditionsPaiement) {
        this.conditionsPaiement = conditionsPaiement;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    public Long getCommandeGenereeId() {
        return commandeGenereeId;
    }

    public void setCommandeGenereeId(Long commandeGenereeId) {
        this.commandeGenereeId = commandeGenereeId;
    }

    public Long getFactureGenereeId() {
        return factureGenereeId;
    }

    public void setFactureGenereeId(Long factureGenereeId) {
        this.factureGenereeId = factureGenereeId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
