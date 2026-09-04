package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutTransfert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransfertStockDTO {
    private Long id;
    private String numeroTransfert;
    private Long depotSourceId;
    private String depotSourceNom;
    private Long depotDestinationId;
    private String depotDestinationNom;
    private LocalDate dateTransfert;
    private LocalDateTime dateExpedition;
    private LocalDate dateReception;
    private StatutTransfert statut;
    private String motif;
    private String notes;
    private Long creeParUserId;
    private String creeParNom;
    private Long valideParUserId;
    private String valideParNom;
    private Long expedieParUserId;
    private String expedieParNom;
    private Long recuParUserId;
    private String recuParNom;
    private List<LigneTransfertStockDTO> lignes = new ArrayList<>();

    public TransfertStockDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroTransfert() { return numeroTransfert; }
    public void setNumeroTransfert(String numeroTransfert) { this.numeroTransfert = numeroTransfert; }

    public Long getDepotSourceId() { return depotSourceId; }
    public void setDepotSourceId(Long depotSourceId) { this.depotSourceId = depotSourceId; }

    public String getDepotSourceNom() { return depotSourceNom; }
    public void setDepotSourceNom(String depotSourceNom) { this.depotSourceNom = depotSourceNom; }

    public Long getDepotDestinationId() { return depotDestinationId; }
    public void setDepotDestinationId(Long depotDestinationId) { this.depotDestinationId = depotDestinationId; }

    public String getDepotDestinationNom() { return depotDestinationNom; }
    public void setDepotDestinationNom(String depotDestinationNom) { this.depotDestinationNom = depotDestinationNom; }

    public LocalDate getDateTransfert() { return dateTransfert; }
    public void setDateTransfert(LocalDate dateTransfert) { this.dateTransfert = dateTransfert; }

    public LocalDateTime getDateExpedition() { return dateExpedition; }
    public void setDateExpedition(LocalDateTime dateExpedition) { this.dateExpedition = dateExpedition; }

    public LocalDate getDateReception() { return dateReception; }
    public void setDateReception(LocalDate dateReception) { this.dateReception = dateReception; }

    public StatutTransfert getStatut() { return statut; }
    public void setStatut(StatutTransfert statut) { this.statut = statut; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getCreeParUserId() { return creeParUserId; }
    public void setCreeParUserId(Long creeParUserId) { this.creeParUserId = creeParUserId; }

    public String getCreeParNom() { return creeParNom; }
    public void setCreeParNom(String creeParNom) { this.creeParNom = creeParNom; }

    public Long getValideParUserId() { return valideParUserId; }
    public void setValideParUserId(Long valideParUserId) { this.valideParUserId = valideParUserId; }

    public String getValideParNom() { return valideParNom; }
    public void setValideParNom(String valideParNom) { this.valideParNom = valideParNom; }

    public Long getExpedieParUserId() { return expedieParUserId; }
    public void setExpedieParUserId(Long expedieParUserId) { this.expedieParUserId = expedieParUserId; }

    public String getExpedieParNom() { return expedieParNom; }
    public void setExpedieParNom(String expedieParNom) { this.expedieParNom = expedieParNom; }

    public Long getRecuParUserId() { return recuParUserId; }
    public void setRecuParUserId(Long recuParUserId) { this.recuParUserId = recuParUserId; }

    public String getRecuParNom() { return recuParNom; }
    public void setRecuParNom(String recuParNom) { this.recuParNom = recuParNom; }

    public List<LigneTransfertStockDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneTransfertStockDTO> lignes) { this.lignes = lignes; }
}
