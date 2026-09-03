package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeCompteFinancier;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CompteFinancierDTO {
    private Long id;
    private String code;
    private String nom;
    private TypeCompteFinancier type;
    private String typeLibelle;
    private BigDecimal soldeActuel;
    private String devise;
    private String numeroCompteRib;
    private String nomBanque;
    private Boolean actif;
    private Long pointDeVenteId;
    private LocalDateTime dateCreation;

    public CompteFinancierDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public TypeCompteFinancier getType() { return type; }
    public void setType(TypeCompteFinancier type) {
        this.type = type;
        if (type != null) this.typeLibelle = type.getLibelle();
    }

    public String getTypeLibelle() { return typeLibelle; }
    public void setTypeLibelle(String typeLibelle) { this.typeLibelle = typeLibelle; }

    public BigDecimal getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(BigDecimal soldeActuel) { this.soldeActuel = soldeActuel; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public String getNumeroCompteRib() { return numeroCompteRib; }
    public void setNumeroCompteRib(String numeroCompteRib) { this.numeroCompteRib = numeroCompteRib; }

    public String getNomBanque() { return nomBanque; }
    public void setNomBanque(String nomBanque) { this.nomBanque = nomBanque; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
