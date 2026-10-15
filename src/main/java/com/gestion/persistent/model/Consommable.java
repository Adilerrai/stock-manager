package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.CategorieConsommable;
import com.gestion.persistent.enums.UniteConsommable;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consommables", uniqueConstraints = {
    @UniqueConstraint(name = "uk_consommables_tenant_ref", columnNames = {"point_de_vente_id", "reference"})
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Consommable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(nullable = false)
    private String reference;

    @Column(name = "code_barre")
    private String codeBarre;

    @Column(nullable = false)
    private String designation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", length = 50, nullable = false)
    private CategorieConsommable categorie = CategorieConsommable.FOURNITURE_BUREAU;

    @Enumerated(EnumType.STRING)
    @Column(name = "unite_mesure", length = 30, nullable = false)
    private UniteConsommable uniteMesure = UniteConsommable.PIECE;

    @Column(name = "stock_actuel", precision = 12, scale = 2, nullable = false)
    private BigDecimal stockActuel = BigDecimal.ZERO;

    @Column(name = "stock_minimum", precision = 12, scale = 2)
    private BigDecimal stockMinimum = BigDecimal.ZERO;

    @Column(name = "dernier_prix_achat", precision = 12, scale = 2)
    private BigDecimal dernierPrixAchat = BigDecimal.ZERO;

    @Column(name = "emplacement")
    private String emplacement;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "derniere_mise_a_jour")
    private LocalDateTime derniereMiseAJour;

    public Consommable() {}

    @PrePersist
    public void prePersist() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant != null) {
            this.pointDeVenteId = tenant;
        } else if (this.pointDeVenteId == null) {
            this.pointDeVenteId = 1L;
        }
        if (this.stockActuel == null) this.stockActuel = BigDecimal.ZERO;
        if (this.stockMinimum == null) this.stockMinimum = BigDecimal.ZERO;
        if (this.dernierPrixAchat == null) this.dernierPrixAchat = BigDecimal.ZERO;
        if (this.dateCreation == null) this.dateCreation = LocalDateTime.now();
        this.derniereMiseAJour = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.derniereMiseAJour = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getCodeBarre() { return codeBarre; }
    public void setCodeBarre(String codeBarre) { this.codeBarre = codeBarre; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CategorieConsommable getCategorie() { return categorie; }
    public void setCategorie(CategorieConsommable categorie) { this.categorie = categorie; }

    public UniteConsommable getUniteMesure() { return uniteMesure; }
    public void setUniteMesure(UniteConsommable uniteMesure) { this.uniteMesure = uniteMesure; }

    public BigDecimal getStockActuel() { return stockActuel != null ? stockActuel : BigDecimal.ZERO; }
    public void setStockActuel(BigDecimal stockActuel) { this.stockActuel = stockActuel; }

    public BigDecimal getStockMinimum() { return stockMinimum != null ? stockMinimum : BigDecimal.ZERO; }
    public void setStockMinimum(BigDecimal stockMinimum) { this.stockMinimum = stockMinimum; }

    public BigDecimal getDernierPrixAchat() { return dernierPrixAchat != null ? dernierPrixAchat : BigDecimal.ZERO; }
    public void setDernierPrixAchat(BigDecimal dernierPrixAchat) { this.dernierPrixAchat = dernierPrixAchat; }

    public String getEmplacement() { return emplacement; }
    public void setEmplacement(String emplacement) { this.emplacement = emplacement; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDerniereMiseAJour() { return derniereMiseAJour; }
    public void setDerniereMiseAJour(LocalDateTime derniereMiseAJour) { this.derniereMiseAJour = derniereMiseAJour; }

    public boolean isEnAlerteStock() {
        return stockMinimum != null && stockActuel != null && stockActuel.compareTo(stockMinimum) <= 0;
    }
}
