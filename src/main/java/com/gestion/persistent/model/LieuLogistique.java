package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypeLieuLogistique;
import jakarta.persistence.*;

/**
 * Port / aéroport / point frontière de destination. point_de_vente_id NULL = référentiel global.
 */
@Entity
@Table(name = "lieux_logistiques")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LieuLogistique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeLieuLogistique type = TypeLieuLogistique.PORT;

    private String pays = "Maroc";

    private Boolean actif = true;

    @Column(name = "point_de_vente_id")
    private Long pointDeVenteId;

    public LieuLogistique() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public TypeLieuLogistique getType() { return type; }
    public void setType(TypeLieuLogistique type) { this.type = type; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
