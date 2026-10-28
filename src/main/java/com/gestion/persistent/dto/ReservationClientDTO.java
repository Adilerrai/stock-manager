package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.StatutReservationClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservationClientDTO {
    private Long id;
    private String numeroReservation;
    private LocalDateTime dateReservation;
    private LocalDate dateLivraisonSouhaitee;
    private Long clientId;
    private String clientNom;
    private String clientIce;
    private String clientTelephone;
    private Long creeParId;
    private String creeParNom;
    private StatutReservationClient statut;
    private String observationsClient;
    private String notesTenant;
    private Long devisGenereId;
    private String numeroDevisGenere;
    private String statutDevisGenere;
    private Long commandeGenereeId;
    private String numeroCommandeGeneree;
    private Long pointDeVenteId;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<LigneReservationClientDTO> lignes = new ArrayList<>();

    public ReservationClientDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroReservation() { return numeroReservation; }
    public void setNumeroReservation(String numeroReservation) { this.numeroReservation = numeroReservation; }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) { this.dateReservation = dateReservation; }

    public LocalDate getDateLivraisonSouhaitee() { return dateLivraisonSouhaitee; }
    public void setDateLivraisonSouhaitee(LocalDate dateLivraisonSouhaitee) { this.dateLivraisonSouhaitee = dateLivraisonSouhaitee; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientIce() { return clientIce; }
    public void setClientIce(String clientIce) { this.clientIce = clientIce; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getCreeParId() { return creeParId; }
    public void setCreeParId(Long creeParId) { this.creeParId = creeParId; }

    public String getCreeParNom() { return creeParNom; }
    public void setCreeParNom(String creeParNom) { this.creeParNom = creeParNom; }

    public StatutReservationClient getStatut() { return statut; }
    public void setStatut(StatutReservationClient statut) { this.statut = statut; }

    public String getObservationsClient() { return observationsClient; }
    public void setObservationsClient(String observationsClient) { this.observationsClient = observationsClient; }

    public String getNotesTenant() { return notesTenant; }
    public void setNotesTenant(String notesTenant) { this.notesTenant = notesTenant; }

    public Long getDevisGenereId() { return devisGenereId; }
    public void setDevisGenereId(Long devisGenereId) { this.devisGenereId = devisGenereId; }

    public String getNumeroDevisGenere() { return numeroDevisGenere; }
    public void setNumeroDevisGenere(String numeroDevisGenere) { this.numeroDevisGenere = numeroDevisGenere; }

    public String getStatutDevisGenere() { return statutDevisGenere; }
    public void setStatutDevisGenere(String statutDevisGenere) { this.statutDevisGenere = statutDevisGenere; }

    public Long getCommandeGenereeId() { return commandeGenereeId; }
    public void setCommandeGenereeId(Long commandeGenereeId) { this.commandeGenereeId = commandeGenereeId; }

    public String getNumeroCommandeGeneree() { return numeroCommandeGeneree; }
    public void setNumeroCommandeGeneree(String numeroCommandeGeneree) { this.numeroCommandeGeneree = numeroCommandeGeneree; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public List<LigneReservationClientDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneReservationClientDTO> lignes) { this.lignes = lignes; }
}
