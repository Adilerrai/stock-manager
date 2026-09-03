package com.gestion.persistent.model;

import com.acommon.persistant.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "objectifs_commerciaux", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"commercial_user_id", "annee", "mois"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ObjectifCommercial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commercial_user_id", nullable = false)
    private User commercial;

    @Column(nullable = false)
    private Integer annee;

    @Column(nullable = false)
    private Integer mois; // 1-12

    @Column(name = "objectif_ca", precision = 15, scale = 2, nullable = false)
    private BigDecimal objectifCA = BigDecimal.ZERO;

    @Column(name = "objectif_marge", precision = 15, scale = 2)
    private BigDecimal objectifMarge = BigDecimal.ZERO;

    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public ObjectifCommercial() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getCommercial() { return commercial; }
    public void setCommercial(User commercial) { this.commercial = commercial; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public Integer getMois() { return mois; }
    public void setMois(Integer mois) { this.mois = mois; }

    public BigDecimal getObjectifCA() { return objectifCA; }
    public void setObjectifCA(BigDecimal objectifCA) { this.objectifCA = objectifCA; }

    public BigDecimal getObjectifMarge() { return objectifMarge; }
    public void setObjectifMarge(BigDecimal objectifMarge) { this.objectifMarge = objectifMarge; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
