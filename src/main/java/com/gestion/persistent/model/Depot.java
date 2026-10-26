package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "depots")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Depot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "code", length = 50)
    private String code;

    private String description;

    @Column(nullable = false)
    private String adresse;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    public void prePersist() {
        if (this.pointDeVenteId == null) {
            Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
            this.pointDeVenteId = (tenant != null) ? tenant : 1L;
        }
    }

    // Constructors, getters, setters
    public Depot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Transient
    public String getCodeOrDefault() {
        if (code != null && !code.isBlank()) {
            return code.trim().toUpperCase();
        }
        if (description != null && !description.isBlank() && description.trim().length() <= 10) {
            return description.trim().toUpperCase();
        }
        if (nom != null) {
            String upper = nom.toUpperCase();
            if (upper.contains("TACHAROUK")) return "TAC";
            if (upper.contains("BERRECHID")) return "BER";
            if (upper.contains("SIT") || upper.contains("CENTRAL") || upper.contains("PRINCIPAL")) return "WH";
            String cleaned = upper.replaceAll("[^A-Z0-9]", "");
            if (cleaned.length() >= 3) return cleaned.substring(0, 3);
            if (!cleaned.isEmpty()) return cleaned;
        }
        return "DEP";
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
