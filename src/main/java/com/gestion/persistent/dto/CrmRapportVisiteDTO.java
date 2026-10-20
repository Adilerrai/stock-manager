package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutRapportVisite;
import java.time.LocalDateTime;
import java.util.List;

public class CrmRapportVisiteDTO {
    private Long id;
    private Long pointDeVenteId;
    private Long visiteId;
    private String visiteLieu;
    private LocalDateTime visiteDate;
    private String visiteType;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long contactId;
    private String contactNom;
    private Long commercialId;
    private String commercialNom;
    private StatutRapportVisite statut;
    private String resume;
    private String interlocuteursPresents;
    private String besoinsIdentifies;
    private String objectionsSoulevees;
    private String resultat;
    private String prochaineEtape;
    private Integer dureeMinutes;
    private LocalDateTime soumisLe;
    private Long valideParUserId;
    private String valideParNom;
    private LocalDateTime valideLe;
    private String motifRejet;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private boolean verrouille;
    private List<CrmTacheDTO> actionsSuivi;

    public CrmRapportVisiteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getVisiteId() { return visiteId; }
    public void setVisiteId(Long visiteId) { this.visiteId = visiteId; }

    public String getVisiteLieu() { return visiteLieu; }
    public void setVisiteLieu(String visiteLieu) { this.visiteLieu = visiteLieu; }

    public LocalDateTime getVisiteDate() { return visiteDate; }
    public void setVisiteDate(LocalDateTime visiteDate) { this.visiteDate = visiteDate; }

    public String getVisiteType() { return visiteType; }
    public void setVisiteType(String visiteType) { this.visiteType = visiteType; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getContactNom() { return contactNom; }
    public void setContactNom(String contactNom) { this.contactNom = contactNom; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public String getCommercialNom() { return commercialNom; }
    public void setCommercialNom(String commercialNom) { this.commercialNom = commercialNom; }

    public StatutRapportVisite getStatut() { return statut; }
    public void setStatut(StatutRapportVisite statut) { this.statut = statut; }

    public String getResume() { return resume; }
    public void setResume(String resume) { this.resume = resume; }

    public String getInterlocuteursPresents() { return interlocuteursPresents; }
    public void setInterlocuteursPresents(String interlocuteursPresents) { this.interlocuteursPresents = interlocuteursPresents; }

    public String getBesoinsIdentifies() { return besoinsIdentifies; }
    public void setBesoinsIdentifies(String besoinsIdentifies) { this.besoinsIdentifies = besoinsIdentifies; }

    public String getObjectionsSoulevees() { return objectionsSoulevees; }
    public void setObjectionsSoulevees(String objectionsSoulevees) { this.objectionsSoulevees = objectionsSoulevees; }

    public String getResultat() { return resultat; }
    public void setResultat(String resultat) { this.resultat = resultat; }

    public String getProchaineEtape() { return prochaineEtape; }
    public void setProchaineEtape(String prochaineEtape) { this.prochaineEtape = prochaineEtape; }

    public Integer getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public LocalDateTime getSoumisLe() { return soumisLe; }
    public void setSoumisLe(LocalDateTime soumisLe) { this.soumisLe = soumisLe; }

    public Long getValideParUserId() { return valideParUserId; }
    public void setValideParUserId(Long valideParUserId) { this.valideParUserId = valideParUserId; }

    public String getValideParNom() { return valideParNom; }
    public void setValideParNom(String valideParNom) { this.valideParNom = valideParNom; }

    public LocalDateTime getValideLe() { return valideLe; }
    public void setValideLe(LocalDateTime valideLe) { this.valideLe = valideLe; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public boolean isVerrouille() { return verrouille; }
    public void setVerrouille(boolean verrouille) { this.verrouille = verrouille; }

    public List<CrmTacheDTO> getActionsSuivi() { return actionsSuivi; }
    public void setActionsSuivi(List<CrmTacheDTO> actionsSuivi) { this.actionsSuivi = actionsSuivi; }
}
