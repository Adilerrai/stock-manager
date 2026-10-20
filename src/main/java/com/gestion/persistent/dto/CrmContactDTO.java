package com.gestion.persistent.dto;

import com.gestion.persistent.enums.SourceAcquisition;
import com.gestion.persistent.enums.StatutContact;
import java.time.LocalDateTime;

public class CrmContactDTO {
    private Long id;
    private Long pointDeVenteId;
    private Long entrepriseId;
    private String entrepriseNom;
    private String nom;
    private String prenom;
    private String nomComplet;
    private String fonction;
    private String email;
    private String telephone;
    private String mobile;
    private SourceAcquisition sourceAcquisition;
    private StatutContact statut;
    private Long commercialResponsableId;
    private String commercialResponsableNom;
    private Boolean principal;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public CrmContactDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

    public String getFonction() { return fonction; }
    public void setFonction(String fonction) { this.fonction = fonction; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public SourceAcquisition getSourceAcquisition() { return sourceAcquisition; }
    public void setSourceAcquisition(SourceAcquisition sourceAcquisition) { this.sourceAcquisition = sourceAcquisition; }

    public StatutContact getStatut() { return statut; }
    public void setStatut(StatutContact statut) { this.statut = statut; }

    public Long getCommercialResponsableId() { return commercialResponsableId; }
    public void setCommercialResponsableId(Long commercialResponsableId) { this.commercialResponsableId = commercialResponsableId; }

    public String getCommercialResponsableNom() { return commercialResponsableNom; }
    public void setCommercialResponsableNom(String commercialResponsableNom) { this.commercialResponsableNom = commercialResponsableNom; }

    public Boolean getPrincipal() { return principal; }
    public void setPrincipal(Boolean principal) { this.principal = principal; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
