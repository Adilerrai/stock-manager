package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutLivraison;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BonLivraisonClientDTO {
    private Long id;
    private String numeroBl;
    private LocalDateTime dateBl;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long commandeClientId;
    private String commandeClientNumero;
    private StatutLivraison statut;
    private BigDecimal montantTotal;
    private String observations;
    private Long pointDeVenteId;
    private Long factureId;
    private String factureNumero;
    private Boolean facturé;
    private List<LigneBonLivraisonClientDTO> lignes = new ArrayList<>();

    public BonLivraisonClientDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public LocalDateTime getDateBl() { return dateBl; }
    public void setDateBl(LocalDateTime dateBl) { this.dateBl = dateBl; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getCommandeClientId() { return commandeClientId; }
    public void setCommandeClientId(Long commandeClientId) { this.commandeClientId = commandeClientId; }

    public String getCommandeClientNumero() { return commandeClientNumero; }
    public void setCommandeClientNumero(String commandeClientNumero) { this.commandeClientNumero = commandeClientNumero; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public String getFactureNumero() { return factureNumero; }
    public void setFactureNumero(String factureNumero) { this.factureNumero = factureNumero; }

    public Boolean getFacturé() { return facturé != null ? facturé : (factureId != null); }
    public void setFacturé(Boolean facturé) { this.facturé = facturé; }

    public List<LigneBonLivraisonClientDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneBonLivraisonClientDTO> lignes) { this.lignes = lignes; }
}
