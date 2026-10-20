package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutEntreprise;
import java.time.LocalDateTime;

public class CrmEntrepriseDTO {
    private Long id;
    private Long pointDeVenteId;
    private String nom;
    private String secteurActivite;
    private String tailleEntreprise;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String telephone;
    private String email;
    private String siteWeb;
    private String ice;
    private StatutEntreprise statut;
    private Long commercialResponsableId;
    private String commercialResponsableNom;
    private Long clientId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private Long nombreContacts;
    private Long nombreOpportunites;
    private Long nombreVisites;

    public CrmEntrepriseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getSecteurActivite() { return secteurActivite; }
    public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }

    public String getTailleEntreprise() { return tailleEntreprise; }
    public void setTailleEntreprise(String tailleEntreprise) { this.tailleEntreprise = tailleEntreprise; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public StatutEntreprise getStatut() { return statut; }
    public void setStatut(StatutEntreprise statut) { this.statut = statut; }

    public Long getCommercialResponsableId() { return commercialResponsableId; }
    public void setCommercialResponsableId(Long commercialResponsableId) { this.commercialResponsableId = commercialResponsableId; }

    public String getCommercialResponsableNom() { return commercialResponsableNom; }
    public void setCommercialResponsableNom(String commercialResponsableNom) { this.commercialResponsableNom = commercialResponsableNom; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public Long getNombreContacts() { return nombreContacts; }
    public void setNombreContacts(Long nombreContacts) { this.nombreContacts = nombreContacts; }

    public Long getNombreOpportunites() { return nombreOpportunites; }
    public void setNombreOpportunites(Long nombreOpportunites) { this.nombreOpportunites = nombreOpportunites; }

    public Long getNombreVisites() { return nombreVisites; }
    public void setNombreVisites(Long nombreVisites) { this.nombreVisites = nombreVisites; }
}
