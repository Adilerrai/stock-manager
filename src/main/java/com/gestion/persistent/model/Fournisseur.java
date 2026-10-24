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

    @Enumerated(EnumType.STRING)
    @Column(name = "type_fournisseur", length = 30)
    private com.gestion.persistent.enums.TypeFournisseur typeFournisseur = com.gestion.persistent.enums.TypeFournisseur.NATIONAL;

    @Column(name = "devise", length = 10)
    private String devise = "MAD";

    @Enumerated(EnumType.STRING)
    @Column(name = "incoterm_defaut", length = 10)
    private com.gestion.persistent.enums.Incoterm incotermDefaut;

    @Column(name = "condition_paiement_defaut_id")
    private Long conditionPaiementDefautId;

    @Column(name = "port_embarquement_defaut", length = 100)
    private String portEmbarquementDefaut;

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

    public com.gestion.persistent.enums.TypeFournisseur getTypeFournisseur() { return typeFournisseur; }
    public void setTypeFournisseur(com.gestion.persistent.enums.TypeFournisseur typeFournisseur) { this.typeFournisseur = typeFournisseur; }

    public String getDevise() { return devise != null ? devise : "MAD"; }
    public void setDevise(String devise) { this.devise = devise; }

    public boolean isInternational() {
        return com.gestion.persistent.enums.TypeFournisseur.INTERNATIONAL.equals(this.typeFournisseur);
    }

    public com.gestion.persistent.enums.Incoterm getIncotermDefaut() { return incotermDefaut; }
    public void setIncotermDefaut(com.gestion.persistent.enums.Incoterm incotermDefaut) { this.incotermDefaut = incotermDefaut; }

    public Long getConditionPaiementDefautId() { return conditionPaiementDefautId; }
    public void setConditionPaiementDefautId(Long conditionPaiementDefautId) { this.conditionPaiementDefautId = conditionPaiementDefautId; }

    public String getPortEmbarquementDefaut() { return portEmbarquementDefaut; }
    public void setPortEmbarquementDefaut(String portEmbarquementDefaut) { this.portEmbarquementDefaut = portEmbarquementDefaut; }
}
