package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutDevis;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DevisDTO {
    private Long id;
    private String numeroDevis;
    private LocalDate dateDevis;
    private LocalDate dateValidite;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long creeParId;
    private String creeParNom;
    private List<LigneDevisDTO> lignes = new ArrayList<>();
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal remiseGlobale;
    private BigDecimal montantFinal;
    private StatutDevis statut;
    private String notes;
    private String conditionsPaiement;
    private Long pointDeVenteId;
    private Long commandeGenereeId;
    private Long factureGenereeId;
    private LocalDateTime dateCreation;

    public DevisDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public LocalDate getDateDevis() { return dateDevis; }
    public void setDateDevis(LocalDate dateDevis) { this.dateDevis = dateDevis; }

    public LocalDate getDateValidite() { return dateValidite; }
    public void setDateValidite(LocalDate dateValidite) { this.dateValidite = dateValidite; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getCreeParId() { return creeParId; }
    public void setCreeParId(Long creeParId) { this.creeParId = creeParId; }

    public String getCreeParNom() { return creeParNom; }
    public void setCreeParNom(String creeParNom) { this.creeParNom = creeParNom; }

    public List<LigneDevisDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneDevisDTO> lignes) { this.lignes = lignes; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public BigDecimal getRemiseGlobale() { return remiseGlobale; }
    public void setRemiseGlobale(BigDecimal remiseGlobale) { this.remiseGlobale = remiseGlobale; }

    public BigDecimal getMontantFinal() { return montantFinal; }
    public void setMontantFinal(BigDecimal montantFinal) { this.montantFinal = montantFinal; }

    public StatutDevis getStatut() { return statut; }
    public void setStatut(StatutDevis statut) { this.statut = statut; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getCommandeGenereeId() { return commandeGenereeId; }
    public void setCommandeGenereeId(Long commandeGenereeId) { this.commandeGenereeId = commandeGenereeId; }

    public Long getFactureGenereeId() { return factureGenereeId; }
    public void setFactureGenereeId(Long factureGenereeId) { this.factureGenereeId = factureGenereeId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
