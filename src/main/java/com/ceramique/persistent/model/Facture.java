package com.ceramique.persistent.model;

import com.acommon.persistant.model.User;
import com.ceramique.persistent.enums.StatutFacture;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "factures")
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroFacture;

    @Column(name = "date_facture", nullable = false)
    private LocalDate dateFacture = LocalDate.now();

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emise_par_user_id", nullable = false)
    private User emisePar;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFacture> lignes = new ArrayList<>();

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
    private StatutFacture statut = StatutFacture.EN_ATTENTE;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL)
    private List<Paiement> paiements = new ArrayList<>();

    @Column(name = "montant_paye", precision = 15, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "montant_restant", precision = 15, scale = 2)
    private BigDecimal montantRestant = BigDecimal.ZERO;


    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    private String notes;

    @Column(name = "conditions_paiement")
    private String conditionsPaiement;

    private Boolean annulee = false;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Column(name = "motif_annulation")
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annulee_par_user_id")
    private User annuleePar;

    @Column(name = "chemin_pdf")
    private String cheminPdf;

    // Constructeurs
    public Facture() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public LocalDate getDateFacture() {
        return dateFacture;
    }

    public void setDateFacture(LocalDate dateFacture) {
        this.dateFacture = dateFacture;
    }

    public LocalDate getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(LocalDate dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Vente getVente() {
        return vente;
    }

    public void setVente(Vente vente) {
        this.vente = vente;
    }

    public User getEmisePar() {
        return emisePar;
    }

    public void setEmisePar(User emisePar) {
        this.emisePar = emisePar;
    }

    public List<LigneFacture> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneFacture> lignes) {
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

    public StatutFacture getStatut() {
        return statut;
    }

    public void setStatut(StatutFacture statut) {
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

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
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

    public Boolean getAnnulee() {
        return annulee;
    }

    public void setAnnulee(Boolean annulee) {
        this.annulee = annulee;
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

    public User getAnnuleePar() {
        return annuleePar;
    }

    public void setAnnuleePar(User annuleePar) {
        this.annuleePar = annuleePar;
    }

    public String getCheminPdf() {
        return cheminPdf;
    }

    public void setCheminPdf(String cheminPdf) {
        this.cheminPdf = cheminPdf;
    }

    // Méthodes utilitaires
    public void addLigne(LigneFacture ligne) {
        lignes.add(ligne);
        ligne.setFacture(this);
    }

    public void removeLigne(LigneFacture ligne) {
        lignes.remove(ligne);
        ligne.setFacture(null);
    }

    public void calculerMontants() {
        this.montantHT = lignes.stream()
                .map(LigneFacture::getMontantHT)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montantTVA = lignes.stream()
                .map(LigneFacture::getMontantTVA)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.montantTTC = montantHT.add(montantTVA);
        this.montantFinal = montantTTC.subtract(remiseGlobale != null ? remiseGlobale : BigDecimal.ZERO);
        this.montantRestant = montantFinal.subtract(montantPaye != null ? montantPaye : BigDecimal.ZERO);
    }

    public boolean estSoldee() {
        return montantRestant.compareTo(BigDecimal.ZERO) <= 0;
    }

    public boolean estEchue() {
        return dateEcheance != null && LocalDate.now().isAfter(dateEcheance) && !estSoldee();
    }
}

