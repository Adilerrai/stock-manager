package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CanalRelance;
import java.time.LocalDateTime;

public class RelanceClientDTO {
    private Long id;
    private Long clientId;
    private String clientNom;
    private Long factureId;
    private String factureNumero;
    private LocalDateTime dateRelance;
    private CanalRelance canal;
    private String canalLibelle;
    private String interlocuteur;
    private String commentaire;
    private Long effectueParUserId;
    private String effectueParNom;

    public RelanceClientDTO() {}

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

    public LocalDateTime getDateRelance() { return dateRelance; }
    public void setDateRelance(LocalDateTime dateRelance) { this.dateRelance = dateRelance; }

    public CanalRelance getCanal() { return canal; }
    public void setCanal(CanalRelance canal) {
        this.canal = canal;
        if (canal != null) this.canalLibelle = canal.getLibelle();
    }

    public String getCanalLibelle() { return canalLibelle; }
    public void setCanalLibelle(String canalLibelle) { this.canalLibelle = canalLibelle; }

    public String getInterlocuteur() { return interlocuteur; }
    public void setInterlocuteur(String interlocuteur) { this.interlocuteur = interlocuteur; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Long getEffectueParUserId() { return effectueParUserId; }
    public void setEffectueParUserId(Long effectueParUserId) { this.effectueParUserId = effectueParUserId; }

    public String getEffectueParNom() { return effectueParNom; }
    public void setEffectueParNom(String effectueParNom) { this.effectueParNom = effectueParNom; }
}
