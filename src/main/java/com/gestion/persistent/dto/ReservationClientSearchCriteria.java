package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutReservationClient;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Critères de recherche multi-critères pour les réservations d'articles client avec pagination côté serveur.
 */
public class ReservationClientSearchCriteria {
    private String numeroReservation;
    private Long clientId;
    private String clientNom;
    private String clientIce;
    private StatutReservationClient statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private LocalDate dateLivraisonSouhaiteeDebut;
    private LocalDate dateLivraisonSouhaiteeFin;
    private Long devisGenereId;
    private Long commandeGenereeId;
    private Long societeId;
    private Boolean inclureAnnulees;

    public ReservationClientSearchCriteria() {}

    public String getNumeroReservation() { return numeroReservation; }
    public void setNumeroReservation(String numeroReservation) { this.numeroReservation = numeroReservation; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientIce() { return clientIce; }
    public void setClientIce(String clientIce) { this.clientIce = clientIce; }

    public StatutReservationClient getStatut() { return statut; }
    public void setStatut(StatutReservationClient statut) { this.statut = statut; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public LocalDate getDateLivraisonSouhaiteeDebut() { return dateLivraisonSouhaiteeDebut; }
    public void setDateLivraisonSouhaiteeDebut(LocalDate dateLivraisonSouhaiteeDebut) { this.dateLivraisonSouhaiteeDebut = dateLivraisonSouhaiteeDebut; }

    public LocalDate getDateLivraisonSouhaiteeFin() { return dateLivraisonSouhaiteeFin; }
    public void setDateLivraisonSouhaiteeFin(LocalDate dateLivraisonSouhaiteeFin) { this.dateLivraisonSouhaiteeFin = dateLivraisonSouhaiteeFin; }

    public Long getDevisGenereId() { return devisGenereId; }
    public void setDevisGenereId(Long devisGenereId) { this.devisGenereId = devisGenereId; }

    public Long getCommandeGenereeId() { return commandeGenereeId; }
    public void setCommandeGenereeId(Long commandeGenereeId) { this.commandeGenereeId = commandeGenereeId; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }

    public Boolean getInclureAnnulees() { return inclureAnnulees; }
    public void setInclureAnnulees(Boolean inclureAnnulees) { this.inclureAnnulees = inclureAnnulees; }
}

