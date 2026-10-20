package com.gestion.persistent.dto;

import com.gestion.persistent.enums.PrioriteTache;
import com.gestion.persistent.enums.StatutTache;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CrmTacheDTO {
    private Long id;
    private Long pointDeVenteId;
    private String titre;
    private String description;
    private Long visiteId;
    private Long rapportId;
    private Long opportuniteId;
    private String opportuniteTitre;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long contactId;
    private String contactNom;
    private Long assigneAUserId;
    private String assigneANom;
    private Long creeParUserId;
    private String creeParNom;
    private PrioriteTache priorite;
    private StatutTache statut;
    private LocalDate dateEcheance;
    private LocalDateTime dateRealisation;
    private LocalDateTime dateCreation;
    private boolean enRetard;

    public CrmTacheDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public Long getRapportId() { return rapportId; }
    public void setRapportId(Long rapportId) { this.rapportId = rapportId; }

    public Long getOpportuniteId() { return opportuniteId; }
    public void setOpportuniteId(Long opportuniteId) { this.opportuniteId = opportuniteId; }

    public String getOpportuniteTitre() { return opportuniteTitre; }
    public void setOpportuniteTitre(String opportuniteTitre) { this.opportuniteTitre = opportuniteTitre; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }

    public Long getAssigneAUserId() { return assigneAUserId; }
    public void setAssigneAUserId(Long assigneAUserId) { this.assigneAUserId = assigneAUserId; }

    public String getAssigneANom() { return assigneANom; }
    public void setAssigneANom(String assigneANom) { this.assigneANom = assigneANom; }

    public Long getCreeParUserId() { return creeParUserId; }
    public void setCreeParUserId(Long creeParUserId) { this.creeParUserId = creeParUserId; }

    public String getCreeParNom() { return creeParNom; }
    public void setCreeParNom(String creeParNom) { this.creeParNom = creeParNom; }

    public PrioriteTache getPriorite() { return priorite; }
    public void setPriorite(PrioriteTache priorite) { this.priorite = priorite; }

    public StatutTache getStatut() { return statut; }
    public void setStatut(StatutTache statut) { this.statut = statut; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public LocalDateTime getDateRealisation() { return dateRealisation; }
    public void setDateRealisation(LocalDateTime dateRealisation) { this.dateRealisation = dateRealisation; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isEnRetard() { return enRetard; }
    public void setEnRetard(boolean enRetard) { this.enRetard = enRetard; }
}
