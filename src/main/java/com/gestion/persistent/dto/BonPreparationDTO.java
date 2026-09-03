package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutPreparation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BonPreparationDTO {
    private Long id;
    private String numeroPreparation;
    private Long commandeClientId;
    private String commandeClientNumero;
    private Long clientId;
    private String clientNom;
    private LocalDateTime dateCreation;
    private LocalDateTime datePreparation;
    private StatutPreparation statut;
    private String statutLibelle;
    private Long magasinierUserId;
    private String magasinierNom;
    private String notes;
    private List<LigneBonPreparationDTO> lignes = new ArrayList<>();

    public BonPreparationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPreparation() { return numeroPreparation; }
    public void setNumeroPreparation(String numeroPreparation) { this.numeroPreparation = numeroPreparation; }

    public Long getCommandeClientId() { return commandeClientId; }
    public void setCommandeClientId(Long commandeClientId) { this.commandeClientId = commandeClientId; }

    public String getCommandeClientNumero() { return commandeClientNumero; }
    public void setCommandeClientNumero(String commandeClientNumero) { this.commandeClientNumero = commandeClientNumero; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDatePreparation() { return datePreparation; }
    public void setDatePreparation(LocalDateTime datePreparation) { this.datePreparation = datePreparation; }

    public StatutPreparation getStatut() { return statut; }
    public void setStatut(StatutPreparation statut) {
        this.statut = statut;
        if (statut != null) this.statutLibelle = statut.getLibelle();
    }

    public String getStatutLibelle() { return statutLibelle; }
    public void setStatutLibelle(String statutLibelle) { this.statutLibelle = statutLibelle; }

    public Long getMagasinierUserId() { return magasinierUserId; }
    public void setMagasinierUserId(Long magasinierUserId) { this.magasinierUserId = magasinierUserId; }

    public String getMagasinierNom() { return magasinierNom; }
    public void setMagasinierNom(String magasinierNom) { this.magasinierNom = magasinierNom; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<LigneBonPreparationDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneBonPreparationDTO> lignes) { this.lignes = lignes; }
}
