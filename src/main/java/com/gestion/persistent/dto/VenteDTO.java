package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutVente;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VenteDTO {
    private Long id;
    private String numeroTicket;
    private LocalDateTime dateVente;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long vendeurId;
    private String vendeurNom;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private BigDecimal remiseGlobale;
    private BigDecimal montantFinal;
    private StatutVente statut;
    private BigDecimal montantPaye;
    private BigDecimal montantRestant;
    private String notes;
    private List<LigneVenteDTO> lignes = new ArrayList<>();

    public VenteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroTicket() { return numeroTicket; }
    public void setNumeroTicket(String numeroTicket) { this.numeroTicket = numeroTicket; }

    public LocalDateTime getDateVente() { return dateVente; }
    public void setDateVente(LocalDateTime dateVente) { this.dateVente = dateVente; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getVendeurId() { return vendeurId; }
    public void setVendeurId(Long vendeurId) { this.vendeurId = vendeurId; }

    public String getVendeurNom() { return vendeurNom; }
    public void setVendeurNom(String vendeurNom) { this.vendeurNom = vendeurNom; }

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

    public StatutVente getStatut() { return statut; }
    public void setStatut(StatutVente statut) { this.statut = statut; }

    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }

    public BigDecimal getMontantRestant() { return montantRestant; }
    public void setMontantRestant(BigDecimal montantRestant) { this.montantRestant = montantRestant; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<LigneVenteDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneVenteDTO> lignes) { this.lignes = lignes; }
}
