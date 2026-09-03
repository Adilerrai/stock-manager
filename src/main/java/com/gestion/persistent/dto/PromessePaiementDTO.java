package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutPromesse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PromessePaiementDTO {
    private Long id;
    private Long clientId;
    private String clientNom;
    private Long factureId;
    private String factureNumero;
    private LocalDateTime datePromesse;
    private LocalDate dateEcheancePromise;
    private BigDecimal montantPromis;
    private StatutPromesse statut;
    private String statutLibelle;
    private String notes;
    private Long enregistreParUserId;
    private String enregistreParNom;
    private Boolean estEnRetard = false;

    public PromessePaiementDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public String getFactureNumero() { return factureNumero; }
    public void setFactureNumero(String factureNumero) { this.factureNumero = factureNumero; }

    public LocalDateTime getDatePromesse() { return datePromesse; }
    public void setDatePromesse(LocalDateTime datePromesse) { this.datePromesse = datePromesse; }

    public LocalDate getDateEcheancePromise() { return dateEcheancePromise; }
    public void setDateEcheancePromise(LocalDate dateEcheancePromise) { this.dateEcheancePromise = dateEcheancePromise; }

    public BigDecimal getMontantPromis() { return montantPromis; }
    public void setMontantPromis(BigDecimal montantPromis) { this.montantPromis = montantPromis; }

    public StatutPromesse getStatut() { return statut; }
    public void setStatut(StatutPromesse statut) {
        this.statut = statut;
        if (statut != null) this.statutLibelle = statut.getLibelle();
    }

    public String getStatutLibelle() { return statutLibelle; }
    public void setStatutLibelle(String statutLibelle) { this.statutLibelle = statutLibelle; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getEnregistreParUserId() { return enregistreParUserId; }
    public void setEnregistreParUserId(Long enregistreParUserId) { this.enregistreParUserId = enregistreParUserId; }

    public String getEnregistreParNom() { return enregistreParNom; }
    public void setEnregistreParNom(String enregistreParNom) { this.enregistreParNom = enregistreParNom; }

    public Boolean getEstEnRetard() { return estEnRetard; }
    public void setEstEnRetard(Boolean estEnRetard) { this.estEnRetard = estEnRetard; }
}
