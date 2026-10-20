package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutVisite;
import com.gestion.persistent.enums.TypeVisite;
import java.time.LocalDateTime;

public class CrmVisiteDTO {
    private Long id;
    private Long pointDeVenteId;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long contactId;
    private String contactNomComplet;
    private Long opportuniteId;
    private String opportuniteTitre;
    private Long commercialId;
    private String commercialNom;
    private LocalDateTime dateVisite;
    private String lieu;
    private TypeVisite typeVisite;
    private String objectif;
    private StatutVisite statut;
    private String motifAnnulation;
    private LocalDateTime dateCreation;
    private Long rapportId;
    private String rapportStatut;

    public CrmVisiteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getContactNomComplet() { return contactNomComplet; }
    public void setContactNomComplet(String contactNomComplet) { this.contactNomComplet = contactNomComplet; }

    public Long getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(Long opportuniteId) { this.opportuniteId = opportuniteId; }

    public String getOpportuniteTitre() { return opportuniteTitre; }
    public void setOpportuniteTitre(String opportuniteTitre) { this.opportuniteTitre = opportuniteTitre; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public String getCommercialNom() { return commercialNom; }
    public void setCommercialNom(String commercialNom) { this.commercialNom = commercialNom; }

    public LocalDateTime getDateVisite() { return dateVisite; }
    public void setDateVisite(LocalDateTime dateVisite) { this.dateVisite = dateVisite; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public TypeVisite getTypeVisite() { return typeVisite; }
    public void setTypeVisite(TypeVisite typeVisite) { this.typeVisite = typeVisite; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    public StatutVisite getStatut() { return statut; }
    public void setStatut(StatutVisite statut) { this.statut = statut; }

    public String getMotifAnnulation() { return motifAnnulation; }
    public void setMotifAnnulation(String motifAnnulation) { this.motifAnnulation = motifAnnulation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Long getRapportId() { return rapportId; }
    public void setRapportId(Long rapportId) { this.rapportId = rapportId; }

    public String getRapportStatut() { return rapportStatut; }
    public void setRapportStatut(String rapportStatut) { this.rapportStatut = rapportStatut; }
}
