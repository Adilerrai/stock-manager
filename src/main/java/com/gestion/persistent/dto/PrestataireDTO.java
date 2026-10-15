package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypePrestation;
import java.time.LocalDateTime;

public class PrestataireDTO {

    private Long id;
    private Long pointDeVenteId;
    private String raisonSociale;
    private String contactNom;
    private String ice;
    private String telephone;
    private String email;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private TypePrestation typePrestation;
    private String typePrestationLibelle;
    private String registreCommerce;
    private String identifiantFiscal;
    private String patente;
    private String ribBancaire;
    private String banqueNom;
    private Integer delaiPaiementJours;
    private Boolean actif;
    private String notes;
    private LocalDateTime dateCreation;
    private Long totalDepenses;
    private java.math.BigDecimal totalMontantDepenses;

    public PrestataireDTO() {}

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
    public void setTypePrestation(TypePrestation typePrestation) {
        this.typePrestation = typePrestation;
        if (typePrestation != null) {
            this.typePrestationLibelle = typePrestation.getLibelle();
        }
    }

    public String getTypePrestationLibelle() { return typePrestationLibelle; }
    public void setTypePrestationLibelle(String typePrestationLibelle) { this.typePrestationLibelle = typePrestationLibelle; }

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

    public Long getTotalDepenses() { return totalDepenses; }
    public void setTotalDepenses(Long totalDepenses) { this.totalDepenses = totalDepenses; }

    public java.math.BigDecimal getTotalMontantDepenses() { return totalMontantDepenses; }
    public void setTotalMontantDepenses(java.math.BigDecimal totalMontantDepenses) { this.totalMontantDepenses = totalMontantDepenses; }
}
