package com.gestion.persistent.model;

import com.acommon.persistant.model.TenantContext;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.TypePrestation;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prestataires")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Prestataire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Column(name = "raison_sociale", nullable = false)
    private String raisonSociale;

    @Column(name = "contact_nom")
    private String contactNom;

    @Column(name = "ice")
    private String ice;

    @Column(name = "telephone")
    private String telephone;

    @Column(name = "email")
    private String email;

    @Column(name = "adresse")
    private String adresse;

    @Column(name = "ville")
    private String ville;

    @Column(name = "code_postal")
    private String codePostal;

    @Column(name = "pays")
    private String pays = "Maroc";

    @Enumerated(EnumType.STRING)
    @Column(name = "type_prestation", length = 50)
    private TypePrestation typePrestation = TypePrestation.AUTRE;

    @Column(name = "registre_commerce")
    private String registreCommerce;

    @Column(name = "identifiant_fiscal")
    private String identifiantFiscal;

    @Column(name = "patente")
    private String patente;

    @Column(name = "rib_bancaire")
    private String ribBancaire;

    @Column(name = "banque_nom")
    private String banqueNom;

    @Column(name = "delai_paiement_jours")
    private Integer delaiPaiementJours = 30;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Prestataire() {}

    @PrePersist
    @PreUpdate
    public void prePersist() {
        Long tenant = TenantContext.getCurrentTenant();
        if (tenant != null) {
            this.pointDeVenteId = tenant;
        } else if (this.pointDeVenteId == null) {
            this.pointDeVenteId = 1L;
        }
        if (this.dateCreation == null) {
            this.dateCreation = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public TypePrestation getTypePrestation() { return typePrestation; }
    public void setTypePrestation(TypePrestation typePrestation) { this.typePrestation = typePrestation; }

    public String getRegistreCommerce() { return registreCommerce; }
    public void setRegistreCommerce(String registreCommerce) { this.registreCommerce = registreCommerce; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getRibBancaire() { return ribBancaire; }
    public void setRibBancaire(String ribBancaire) { this.ribBancaire = ribBancaire; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

    public Integer getDelaiPaiementJours() { return delaiPaiementJours; }
    public void setDelaiPaiementJours(Integer delaiPaiementJours) { this.delaiPaiementJours = delaiPaiementJours; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
