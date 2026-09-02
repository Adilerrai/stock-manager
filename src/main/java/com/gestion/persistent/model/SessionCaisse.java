package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutSessionCaisse;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sessions_caisse")
public class SessionCaisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "date_ouverture", nullable = false)
    private LocalDateTime dateOuverture = LocalDateTime.now();

    @Column(name = "date_cloture")
    private LocalDateTime dateCloture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_user_id", nullable = false)
    private User caissier;

    @Column(name = "fond_de_caisse_initial", precision = 15, scale = 2, nullable = false)
    private BigDecimal fondDeCaisseInitial = BigDecimal.ZERO;

    @Column(name = "total_ventes", precision = 15, scale = 2)
    private BigDecimal totalVentes = BigDecimal.ZERO;

    @Column(name = "total_especes", precision = 15, scale = 2)
    private BigDecimal totalEspeces = BigDecimal.ZERO;

    @Column(name = "total_carte", precision = 15, scale = 2)
    private BigDecimal totalCarte = BigDecimal.ZERO;

    @Column(name = "total_cheque", precision = 15, scale = 2)
    private BigDecimal totalCheque = BigDecimal.ZERO;

    @Column(name = "total_virement", precision = 15, scale = 2)
    private BigDecimal totalVirement = BigDecimal.ZERO;

    @Column(name = "total_credit", precision = 15, scale = 2)
    private BigDecimal totalCredit = BigDecimal.ZERO;

    @Column(name = "montant_theorique_cloture", precision = 15, scale = 2)
    private BigDecimal montantTheoriqueCloture = BigDecimal.ZERO;

    @Column(name = "montant_reel_cloture", precision = 15, scale = 2)
    private BigDecimal montantReelCloture = BigDecimal.ZERO;

    @Column(name = "ecart_caisse", precision = 15, scale = 2)
    private BigDecimal ecartCaisse = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutSessionCaisse statut = StatutSessionCaisse.OUVERTE;

    private String notes;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    public SessionCaisse() {
    }

    public void calculerTotaux() {
        if (fondDeCaisseInitial == null) fondDeCaisseInitial = BigDecimal.ZERO;
        if (totalEspeces == null) totalEspeces = BigDecimal.ZERO;
        if (montantReelCloture == null) montantReelCloture = BigDecimal.ZERO;

        this.montantTheoriqueCloture = fondDeCaisseInitial.add(totalEspeces);
        this.ecartCaisse = montantReelCloture.subtract(this.montantTheoriqueCloture);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public User getCaissier() {
        return caissier;
    }

    public void setCaissier(User caissier) {
        this.caissier = caissier;
    }

    public BigDecimal getFondDeCaisseInitial() {
        return fondDeCaisseInitial;
    }

    public void setFondDeCaisseInitial(BigDecimal fondDeCaisseInitial) {
        this.fondDeCaisseInitial = fondDeCaisseInitial;
    }

    public BigDecimal getTotalVentes() {
        return totalVentes;
    }

    public void setTotalVentes(BigDecimal totalVentes) {
        this.totalVentes = totalVentes;
    }

    public BigDecimal getTotalEspeces() {
        return totalEspeces;
    }

    public void setTotalEspeces(BigDecimal totalEspeces) {
        this.totalEspeces = totalEspeces;
    }

    public BigDecimal getTotalCarte() {
        return totalCarte;
    }

    public void setTotalCarte(BigDecimal totalCarte) {
        this.totalCarte = totalCarte;
    }

    public BigDecimal getTotalCheque() {
        return totalCheque;
    }

    public void setTotalCheque(BigDecimal totalCheque) {
        this.totalCheque = totalCheque;
    }

    public BigDecimal getTotalVirement() {
        return totalVirement;
    }

    public void setTotalVirement(BigDecimal totalVirement) {
        this.totalVirement = totalVirement;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public BigDecimal getMontantTheoriqueCloture() {
        return montantTheoriqueCloture;
    }

    public void setMontantTheoriqueCloture(BigDecimal montantTheoriqueCloture) {
        this.montantTheoriqueCloture = montantTheoriqueCloture;
    }

    public BigDecimal getMontantReelCloture() {
        return montantReelCloture;
    }

    public void setMontantReelCloture(BigDecimal montantReelCloture) {
        this.montantReelCloture = montantReelCloture;
    }

    public BigDecimal getEcartCaisse() {
        return ecartCaisse;
    }

    public void setEcartCaisse(BigDecimal ecartCaisse) {
        this.ecartCaisse = ecartCaisse;
    }

    public StatutSessionCaisse getStatut() {
        return statut;
    }

    public void setStatut(StatutSessionCaisse statut) {
        this.statut = statut;
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
}
