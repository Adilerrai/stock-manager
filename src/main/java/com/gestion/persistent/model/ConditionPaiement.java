package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle de conditions de paiement (ex : 30% avance + 70% contre documents).
 * point_de_vente_id NULL = modèle global.
 */
@Entity
@Table(name = "conditions_paiement")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConditionPaiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String libelle;

    @Column(name = "libelle_en", length = 200)
    private String libelleEn;

    private Boolean actif = true;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    @OneToMany(mappedBy = "conditionPaiement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordre ASC")
    private List<ConditionPaiementLigne> lignes = new ArrayList<>();

    public ConditionPaiement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getLibelleEn() { return libelleEn; }
    public void setLibelleEn(String libelleEn) { this.libelleEn = libelleEn; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<ConditionPaiementLigne> getLignes() { return lignes; }
    public void setLignes(List<ConditionPaiementLigne> lignes) { this.lignes = lignes; }
}
