package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutLivraison;
import java.time.LocalDateTime;

public class BonLivraisonClientSearchCriteria {
    private String numeroBL;
    private Long clientId;
    private Long commandeClientId;
    private StatutLivraison statut;
    private Boolean facturee;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    public BonLivraisonClientSearchCriteria() {}

    public String getNumeroBL() { return numeroBL; }
    public void setNumeroBL(String numeroBL) { this.numeroBL = numeroBL; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public Long getCommandeClientId() { return commandeClientId; }
    public void setCommandeClientId(Long commandeClientId) { this.commandeClientId = commandeClientId; }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public Boolean getFacturee() { return facturee; }
    public void setFacturee(Boolean facturee) { this.facturee = facturee; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
}
