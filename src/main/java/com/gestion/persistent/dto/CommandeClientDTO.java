package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutCommandeClient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CommandeClientDTO {
    private Long id;
    private String numeroCommande;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private String clientEmail;
    private String adresseLivraison;
    private StatutCommandeClient statut;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.acommon.config.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime dateCommande;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.acommon.config.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime dateLivraisonPrevue;
    private BigDecimal montantHT;
    private BigDecimal montantTTC;
    private BigDecimal tauxTVA;
    private BigDecimal remiseGlobalePourcentage;
    private BigDecimal remiseGlobaleMontant;
    private Boolean isRecurrente;
    private String frequenceRecurrence;
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = com.acommon.config.FlexibleLocalDateTimeDeserializer.class)
    private LocalDateTime prochaineDateRecurrence;
    private String observations;
    private Long pointDeVenteId;
    private List<LigneCommandeClientDTO> lignesCommande;

    // Constructors
    public CommandeClientDTO() {}

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public StatutCommandeClient getStatut() { return statut; }
    public void setStatut(StatutCommandeClient statut) { this.statut = statut; }

    public List<LigneCommandeClientDTO> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeClientDTO> lignesCommande) { this.lignesCommande = lignesCommande; }

    public String getClientTelephone() {
        return clientTelephone;
    }

    public void setClientTelephone(String clientTelephone) {
        this.clientTelephone = clientTelephone;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }

    public LocalDateTime getDateCommande() {
        return dateCommande;
    }

    public void setDateCommande(LocalDateTime dateCommande) {
        this.dateCommande = dateCommande;
    }

    public LocalDateTime getDateLivraisonPrevue() {
        return dateLivraisonPrevue;
    }

    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) {
        this.dateLivraisonPrevue = dateLivraisonPrevue;
    }

    public BigDecimal getMontantHT() {
        return montantHT;
    }

    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getMontantTTC() {
        return montantTTC;
    }

    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public BigDecimal getTauxTVA() {
        return tauxTVA;
    }

    public void setTauxTVA(BigDecimal tauxTVA) {
        this.tauxTVA = tauxTVA;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public BigDecimal getRemiseGlobalePourcentage() { return remiseGlobalePourcentage != null ? remiseGlobalePourcentage : BigDecimal.ZERO; }
    public void setRemiseGlobalePourcentage(BigDecimal remiseGlobalePourcentage) { this.remiseGlobalePourcentage = remiseGlobalePourcentage; }

    public BigDecimal getRemiseGlobaleMontant() { return remiseGlobaleMontant != null ? remiseGlobaleMontant : BigDecimal.ZERO; }
    public void setRemiseGlobaleMontant(BigDecimal remiseGlobaleMontant) { this.remiseGlobaleMontant = remiseGlobaleMontant; }

    public Boolean getIsRecurrente() { return isRecurrente != null ? isRecurrente : false; }
    public void setIsRecurrente(Boolean isRecurrente) { this.isRecurrente = isRecurrente; }

    public String getFrequenceRecurrence() { return frequenceRecurrence; }
    public void setFrequenceRecurrence(String frequenceRecurrence) { this.frequenceRecurrence = frequenceRecurrence; }

    public LocalDateTime getProchaineDateRecurrence() { return prochaineDateRecurrence; }
    public void setProchaineDateRecurrence(LocalDateTime prochaineDateRecurrence) { this.prochaineDateRecurrence = prochaineDateRecurrence; }
}
