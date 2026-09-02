package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.gestion.persistent.enums.StatutInventaire;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inventaires")
public class Inventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(name = "date_inventaire", nullable = false)
    private LocalDate dateInventaire = LocalDate.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depot_id", nullable = false)
    private Depot depot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_user_id")
    private User responsable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutInventaire statut = StatutInventaire.BROUILLON;

    @OneToMany(mappedBy = "inventaire", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneInventaire> lignes = new ArrayList<>();

    @Column(name = "total_ecart_positif", precision = 15, scale = 2)
    private BigDecimal totalEcartPositif = BigDecimal.ZERO;

    @Column(name = "total_ecart_negatif", precision = 15, scale = 2)
    private BigDecimal totalEcartNegatif = BigDecimal.ZERO;

    @Column(name = "valeur_totale_ecart", precision = 15, scale = 2)
    private BigDecimal valeurTotaleEcart = BigDecimal.ZERO;

    private String notes;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    public Inventaire() {
    }

    public void calculerTotaux() {
        BigDecimal pos = BigDecimal.ZERO;
        BigDecimal neg = BigDecimal.ZERO;
        BigDecimal val = BigDecimal.ZERO;

        if (lignes != null) {
            for (LigneInventaire ligne : lignes) {
                ligne.setInventaire(this);
                ligne.calculerEcart();

                if (ligne.getEcart() != null) {
                    if (ligne.getEcart().compareTo(BigDecimal.ZERO) > 0) {
                        pos = pos.add(ligne.getEcart());
                    } else if (ligne.getEcart().compareTo(BigDecimal.ZERO) < 0) {
                        neg = neg.add(ligne.getEcart().abs());
                    }
                }
                if (ligne.getValeurEcart() != null) {
                    val = val.add(ligne.getValeurEcart());
                }
            }
        }

        this.totalEcartPositif = pos;
        this.totalEcartNegatif = neg;
        this.valeurTotaleEcart = val;
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

    public LocalDate getDateInventaire() {
        return dateInventaire;
    }

    public void setDateInventaire(LocalDate dateInventaire) {
        this.dateInventaire = dateInventaire;
    }

    public Depot getDepot() {
        return depot;
    }

    public void setDepot(Depot depot) {
        this.depot = depot;
    }

    public User getResponsable() {
        return responsable;
    }

    public void setResponsable(User responsable) {
        this.responsable = responsable;
    }

    public StatutInventaire getStatut() {
        return statut;
    }

    public void setStatut(StatutInventaire statut) {
        this.statut = statut;
    }

    public List<LigneInventaire> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneInventaire> lignes) {
        this.lignes = lignes;
    }

    public BigDecimal getTotalEcartPositif() {
        return totalEcartPositif;
    }

    public void setTotalEcartPositif(BigDecimal totalEcartPositif) {
        this.totalEcartPositif = totalEcartPositif;
    }

    public BigDecimal getTotalEcartNegatif() {
        return totalEcartNegatif;
    }

    public void setTotalEcartNegatif(BigDecimal totalEcartNegatif) {
        this.totalEcartNegatif = totalEcartNegatif;
    }

    public BigDecimal getValeurTotaleEcart() {
        return valeurTotaleEcart;
    }

    public void setValeurTotaleEcart(BigDecimal valeurTotaleEcart) {
        this.valeurTotaleEcart = valeurTotaleEcart;
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

    public LocalDateTime getDateValidation() {
        return dateValidation;
    }

    public void setDateValidation(LocalDateTime dateValidation) {
        this.dateValidation = dateValidation;
    }
}
