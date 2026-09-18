package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fournisseurs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Fournisseur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raison_social", nullable = false)
    private String raisonSociale;

    private String adresse;
    private String telephone;
    private String email;
    private String contact;

    @Column(name = "ice")
    private String ice;

    @Column(name = "numero_registre_commerce")
    private String numeroRegistreCommerce;

    @Column(name = "numero_identification_fiscale")
    private String numeroIdentificationFiscale;

    @Column(name = "patente")
    private String patente;

    @Column(name = "rib_bancaire")
    private String ribBancaire;

    @Column(name = "banque_nom")
    private String banqueNom;

    @Column(name = "delai_paiement_jours")
    private Integer delaiPaiementJours = 30;

    @Column(name = "ville")
    private String ville;

    @Column(name = "code_postal")
    private String codePostal;

    @Column(name = "pays")
    private String pays = "Maroc";

    @Column(name = "conditions_paiement")
    private String conditionsPaiement;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        Long tenant = com.acommon.persistant.model.TenantContext.getCurrentTenant();
        if (tenant != null) {
            this.pointDeVenteId = tenant;
        } else if (this.pointDeVenteId == null) {
            this.pointDeVenteId = 1L;
        }
    }

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Fournisseur() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public String getNom() {
        return raisonSociale;
    }

    public void setNom(String nom) {
        this.raisonSociale = nom;
    }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getNumeroRegistreCommerce() { return numeroRegistreCommerce; }
    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) { this.numeroRegistreCommerce = numeroRegistreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getRibBancaire() { return ribBancaire; }
    public void setRibBancaire(String ribBancaire) { this.ribBancaire = ribBancaire; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

    public Integer getDelaiPaiementJours() { return delaiPaiementJours; }
    public void setDelaiPaiementJours(Integer delaiPaiementJours) { this.delaiPaiementJours = delaiPaiementJours; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
