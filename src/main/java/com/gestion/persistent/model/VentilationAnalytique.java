package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ventilations_analytiques")
public class VentilationAnalytique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ligne_ecriture_id", nullable = false)
    private Long ligneEcritureId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "centre_analytique_id", nullable = false)
    private CentreAnalytique centreAnalytique;

    @Column(name = "pourcentage", nullable = false, precision = 6, scale = 2)
    private BigDecimal pourcentage; // Ex: 50.00%

    @Column(name = "montant", nullable = false, precision = 15, scale = 2)
    private BigDecimal montant;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public VentilationAnalytique() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLigneEcritureId() { return ligneEcritureId; }
    public void setLigneEcritureId(Long ligneEcritureId) { this.ligneEcritureId = ligneEcritureId; }

    public CentreAnalytique getCentreAnalytique() { return centreAnalytique; }
    public void setCentreAnalytique(CentreAnalytique centreAnalytique) { this.centreAnalytique = centreAnalytique; }

    public BigDecimal getPourcentage() { return pourcentage; }
    public void setPourcentage(BigDecimal pourcentage) { this.pourcentage = pourcentage; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
