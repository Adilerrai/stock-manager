package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutAvoir;
import com.gestion.persistent.enums.TypeAvoir;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avoirs")
public class Avoir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroAvoir;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAvoir typeAvoir = TypeAvoir.CLIENT;

    @Column(name = "facture_origine_id")
    private Long factureOrigineId;

    @Column(name = "numero_facture_origine")
    private String numeroFactureOrigine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_user_id")
    private User creePar;

    @OneToMany(mappedBy = "avoir", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneAvoir> lignes = new ArrayList<>();

    @Column(name = "date_avoir", nullable = false)
    private LocalDate dateAvoir = LocalDate.now();

    @Column(name = "montant_ht", precision = 15, scale = 2)
    private BigDecimal montantHT = BigDecimal.ZERO;

    @Column(name = "montant_tva", precision = 15, scale = 2)
    private BigDecimal montantTVA = BigDecimal.ZERO;

    @Column(name = "montant_ttc", precision = 15, scale = 2, nullable = false)
    private BigDecimal montantTTC = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAvoir statut = StatutAvoir.BROUILLON;

    private String motif;

    private String notes;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Avoir() {
    }

    public void calculerTotaux() {
        BigDecimal totalHT = BigDecimal.ZERO;
        BigDecimal totalTVA = BigDecimal.ZERO;
        BigDecimal totalTTC = BigDecimal.ZERO;

        if (lignes != null) {
            for (LigneAvoir ligne : lignes) {
                ligne.setAvoir(this);
                ligne.calculerMontants();
                totalHT = totalHT.add(ligne.getMontantHT());
                totalTVA = totalTVA.add(ligne.getMontantTVA());
                totalTTC = totalTTC.add(ligne.getMontantTTC());
            }
        }

        this.montantHT = totalHT;
        this.montantTVA = totalTVA;
        this.montantTTC = totalTTC;
    }

    public void addLigne(LigneAvoir ligne) {
        lignes.add(ligne);
        ligne.setAvoir(this);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroAvoir() {
        return numeroAvoir;
    }

    public void setNumeroAvoir(String numeroAvoir) {
        this.numeroAvoir = numeroAvoir;
    }

    public TypeAvoir getTypeAvoir() {
        return typeAvoir;
    }

    public void setTypeAvoir(TypeAvoir typeAvoir) {
        this.typeAvoir = typeAvoir;
    }

    public Long getFactureOrigineId() {
        return factureOrigineId;
    }

    public void setFactureOrigineId(Long factureOrigineId) {
        this.factureOrigineId = factureOrigineId;
    }

    public String getNumeroFactureOrigine() {
        return numeroFactureOrigine;
    }

    public void setNumeroFactureOrigine(String numeroFactureOrigine) {
        this.numeroFactureOrigine = numeroFactureOrigine;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public User getCreePar() {
        return creePar;
    }

    public void setCreePar(User creePar) {
        this.creePar = creePar;
    }

    public List<LigneAvoir> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneAvoir> lignes) {
        this.lignes = lignes;
    }

    public LocalDate getDateAvoir() {
        return dateAvoir;
    }

    public void setDateAvoir(LocalDate dateAvoir) {
        this.dateAvoir = dateAvoir;
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

    public StatutAvoir getStatut() {
        return statut;
    }

    public void setStatut(StatutAvoir statut) {
        this.statut = statut;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
