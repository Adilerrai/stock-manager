package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "regles_fiscales_is", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"annee_fiscale", "point_de_vente_id"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegleFiscaleIS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "annee_fiscale", nullable = false)
    private Integer anneeFiscale;

    @Column(name = "seuil_tranche1", precision = 15, scale = 2, nullable = false)
    private BigDecimal seuilTranche1 = new BigDecimal("300000.00");

    @Column(name = "taux_tranche1", precision = 6, scale = 4, nullable = false)
    private BigDecimal tauxTranche1 = new BigDecimal("0.1000"); // 10%

    @Column(name = "seuil_tranche2", precision = 15, scale = 2, nullable = false)
    private BigDecimal seuilTranche2 = new BigDecimal("1000000.00");

    @Column(name = "taux_tranche2", precision = 6, scale = 4, nullable = false)
    private BigDecimal tauxTranche2 = new BigDecimal("0.2000"); // 20%

    @Column(name = "taux_tranche3", precision = 6, scale = 4, nullable = false)
    private BigDecimal tauxTranche3 = new BigDecimal("0.3100"); // 31%

    @Column(name = "taux_cotisation_minimale", precision = 6, scale = 4, nullable = false)
    private BigDecimal tauxCotisationMinimale = new BigDecimal("0.0050"); // 0.50%

    @Column(name = "plancher_cotisation_minimale", precision = 15, scale = 2, nullable = false)
    private BigDecimal plancherCotisationMinimale = new BigDecimal("3000.00");

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour = LocalDateTime.now();

    public RegleFiscaleIS() {}

    public RegleFiscaleIS(Integer anneeFiscale, Long pointDeVenteId) {
        this.anneeFiscale = anneeFiscale;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
        this.dateMiseAJour = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAnneeFiscale() { return anneeFiscale; }
    public void setAnneeFiscale(Integer anneeFiscale) { this.anneeFiscale = anneeFiscale; }

    public BigDecimal getSeuilTranche1() { return seuilTranche1; }
    public void setSeuilTranche1(BigDecimal seuilTranche1) { this.seuilTranche1 = seuilTranche1; }

    public BigDecimal getTauxTranche1() { return tauxTranche1; }
    public void setTauxTranche1(BigDecimal tauxTranche1) { this.tauxTranche1 = tauxTranche1; }

    public BigDecimal getSeuilTranche2() { return seuilTranche2; }
    public void setSeuilTranche2(BigDecimal seuilTranche2) { this.seuilTranche2 = seuilTranche2; }

    public BigDecimal getTauxTranche2() { return tauxTranche2; }
    public void setTauxTranche2(BigDecimal tauxTranche2) { this.tauxTranche2 = tauxTranche2; }

    public BigDecimal getTauxTranche3() { return tauxTranche3; }
    public void setTauxTranche3(BigDecimal tauxTranche3) { this.tauxTranche3 = tauxTranche3; }

    public BigDecimal getTauxCotisationMinimale() { return tauxCotisationMinimale; }
    public void setTauxCotisationMinimale(BigDecimal tauxCotisationMinimale) { this.tauxCotisationMinimale = tauxCotisationMinimale; }

    public BigDecimal getPlancherCotisationMinimale() { return plancherCotisationMinimale; }
    public void setPlancherCotisationMinimale(BigDecimal plancherCotisationMinimale) { this.plancherCotisationMinimale = plancherCotisationMinimale; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
}
