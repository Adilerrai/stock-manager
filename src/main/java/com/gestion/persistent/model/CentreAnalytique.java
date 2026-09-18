package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "centres_analytiques")
public class CentreAnalytique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "axe_id", nullable = false)
    private AxeAnalytique axe;

    @Column(nullable = false, length = 50)
    private String code; // Ex: "PROD", "COMM", "ADMIN", "CHANTIER-01"

    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(length = 50)
    private String type = "COUT"; // COUT, PROFIT, MIXTE

    @Column(length = 150)
    private String responsable;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    public CentreAnalytique() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AxeAnalytique getAxe() { return axe; }
    public void setAxe(AxeAnalytique axe) { this.axe = axe; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getResponsable() { return responsable; }
    public void setResponsable(String responsable) { this.responsable = responsable; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
