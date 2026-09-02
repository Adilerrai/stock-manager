package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutFacture;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FactureDTO {
    private Long id;
    private String numeroFacture;
    private LocalDate dateFacture;
    private LocalDate dateEcheance;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long venteId;
    private Long emiseParUserId;
    private String emiseParNom;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal remiseGlobale;
    private BigDecimal montantFinal;
    private StatutFacture statut;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;
    private String notes;
    private String conditionsReglement;
    private Boolean annulee;
    private LocalDateTime dateCreation;
    private List<LigneFactureDTO> lignes = new ArrayList<>();
    private List<Long> bonLivraisonIds = new ArrayList<>();
    private List<String> bonLivraisonNumeros = new ArrayList<>();

    public FactureDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDate getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDate dateFacture) { this.dateFacture = dateFacture; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getVenteId() { return venteId; }
    public void setVenteId(Long venteId) { this.venteId = venteId; }

    public Long getEmiseParUserId() { return emiseParUserId; }
    public void setEmiseParUserId(Long emiseParUserId) { this.emiseParUserId = emiseParUserId; }

    public String getEmiseParNom() { return emiseParNom; }
    public void setEmiseParNom(String emiseParNom) { this.emiseParNom = emiseParNom; }

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

    public StatutFacture getStatut() { return statut; }
    public void setStatut(StatutFacture statut) { this.statut = statut; }

    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }

    public BigDecimal getMontantRestant() { return montantRestant; }
    public void setMontantRestant(BigDecimal montantRestant) { this.montantRestant = montantRestant; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getConditionsReglement() { return conditionsReglement; }
    public void setConditionsReglement(String conditionsReglement) { this.conditionsReglement = conditionsReglement; }

    public Boolean getAnnulee() { return annulee; }
    public void setAnnulee(Boolean annulee) { this.annulee = annulee; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public List<LigneFactureDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneFactureDTO> lignes) { this.lignes = lignes; }

    public List<Long> getBonLivraisonIds() { return bonLivraisonIds; }
    public void setBonLivraisonIds(List<Long> bonLivraisonIds) { this.bonLivraisonIds = bonLivraisonIds; }

    public List<String> getBonLivraisonNumeros() { return bonLivraisonNumeros; }
    public void setBonLivraisonNumeros(List<String> bonLivraisonNumeros) { this.bonLivraisonNumeros = bonLivraisonNumeros; }
}
