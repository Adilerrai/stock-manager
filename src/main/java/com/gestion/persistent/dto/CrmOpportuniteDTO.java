package com.gestion.persistent.dto;

import com.gestion.persistent.enums.EtapeOpportunite;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CrmOpportuniteDTO {
    private Long id;
    private Long pointDeVenteId;
    private String titre;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long contactId;
    private String contactNomComplet;
    private Long commercialId;
    private String commercialNom;
    private BigDecimal montantEstime;
    private Integer probabilite;
    private LocalDate dateCloturePrevue;
    private EtapeOpportunite etape;
    private String motifPerte;
    private String notes;
    private Long devisId;
    private LocalDateTime dateClotureEffective;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;

    public CrmOpportuniteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Long getEntrepriseId() { return entrepriseId; }
    public void setEntrepriseId(Long entrepriseId) { this.entrepriseId = entrepriseId; }

    public String getEntrepriseNom() { return entrepriseNom; }
    public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }

    public Long getContactId() { return contactId; }
    public void setContactId(Long contactId) { this.contactId = contactId; }

    public String getContactNomComplet() { return contactNomComplet; }
    public void setContactNomComplet(String contactNomComplet) { this.contactNomComplet = contactNomComplet; }

    public Long getCommercialId() { return commercialId; }
    public void setCommercialId(Long commercialId) { this.commercialId = commercialId; }

    public String getCommercialNom() { return commercialNom; }
    public void setCommercialNom(String commercialNom) { this.commercialNom = commercialNom; }

    public BigDecimal getMontantEstime() { return montantEstime; }
    public void setMontantEstime(BigDecimal montantEstime) { this.montantEstime = montantEstime; }

    public Integer getProbabilite() { return probabilite; }
    public void setProbabilite(Integer probabilite) { this.probabilite = probabilite; }

    public LocalDate getDateCloturePrevue() { return dateCloturePrevue; }
    public void setDateCloturePrevue(LocalDate dateCloturePrevue) { this.dateCloturePrevue = dateCloturePrevue; }

    public EtapeOpportunite getEtape() { return etape; }
    public void setEtape(EtapeOpportunite etape) { this.etape = etape; }

    public String getMotifPerte() { return motifPerte; }
    public void setMotifPerte(String motifPerte) { this.motifPerte = motifPerte; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getDevisId() { return devisId; }
    public void setDevisId(Long devisId) { this.devisId = devisId; }

    public LocalDateTime getDateClotureEffective() { return dateClotureEffective; }
    public void setDateClotureEffective(LocalDateTime dateClotureEffective) { this.dateClotureEffective = dateClotureEffective; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
