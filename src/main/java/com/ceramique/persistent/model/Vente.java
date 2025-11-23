package com.ceramique.persistent.model;

import com.acommon.persistant.model.User;
import com.ceramique.persistent.enums.StatutVente;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventes")
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroTicket;

    @Column(name = "date_vente", nullable = false)
    private LocalDateTime dateVente = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private User vendeur;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneVente> lignes = new ArrayList<>();

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
    private StatutVente statut = StatutVente.EN_COURS;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL)
    private List<Paiement> paiements = new ArrayList<>();

    @Column(name = "montant_paye", precision = 15, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "montant_restant", precision = 15, scale = 2)
    private BigDecimal montantRestant = BigDecimal.ZERO;

    @OneToOne(mappedBy = "vente")
    private Facture facture;

    private String notes;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_user_id")
    private User annulePar;

    // Constructeurs
    public Vente() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroTicket() {
        return numeroTicket;
    }

    public void setNumeroTicket(String numeroTicket) {
        this.numeroTicket = numeroTicket;
    }

    public LocalDateTime getDateVente() {
        return dateVente;
    }

    public void setDateVente(LocalDateTime dateVente) {
        this.dateVente = dateVente;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public User getVendeur() {
        return vendeur;
    }

    public void setVendeur(User vendeur) {
        this.vendeur = vendeur;
    }

    public List<LigneVente> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneVente> lignes) {
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

    public StatutVente getStatut() {
        return statut;
    }

    public void setStatut(StatutVente statut) {
        this.statut = statut;
    }

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(BigDecimal montantPaye) {
        this.montantPaye = montantPaye;
    }

    public BigDecimal getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(BigDecimal montantRestant) {
        this.montantRestant = montantRestant;
    }

    public Facture getFacture() {
        return facture;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getDateAnnulation() {
        return dateAnnulation;
    }

    public void setDateAnnulation(LocalDateTime dateAnnulation) {
        this.dateAnnulation = dateAnnulation;
    }

    public String getMotifAnnulation() {
        return motifAnnulation;
    }

    public void setMotifAnnulation(String motifAnnulation) {
        this.motifAnnulation = motifAnnulation;
    }

    public User getAnnulePar() {
        return annulePar;
    }

    public void setAnnulePar(User annulePar) {
        this.annulePar = annulePar;
    }

    // Méthodes utilitaires
    public void addLigne(LigneVente ligne) {
        lignes.add(ligne);
        ligne.setVente(this);
    }

    public void removeLigne(LigneVente ligne) {
        lignes.remove(ligne);
        ligne.setVente(null);
    }

    public void calculerMontants() {
        this.montantHT = lignes.stream()
                .map(LigneVente::getMontantHT)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montantTVA = lignes.stream()
                .map(LigneVente::getMontantTVA)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montantTTC = montantHT.add(montantTVA);
        this.montantFinal = montantTTC.subtract(remiseGlobale != null ? remiseGlobale : BigDecimal.ZERO);
        this.montantRestant = montantFinal.subtract(montantPaye != null ? montantPaye : BigDecimal.ZERO);
    }

    public boolean estSoldee() {
        return montantRestant.compareTo(BigDecimal.ZERO) <= 0;
    }
}

