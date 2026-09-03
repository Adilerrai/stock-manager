package com.gestion.persistent.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class AnomalieDTO {
    private String type;         // "CREDIT_DEPASSE", "FACTURE_ECHUE", "STOCK_NEGATIF", "STOCK_BAS", "ECART_CAISSE", etc.
    private String severite;     // "CRITIQUE", "AVERTISSEMENT", "INFO"
    private String titre;        // Court titre descriptif
    private String message;      // Message détaillé
    private String entite;       // "CLIENT", "FACTURE", "PRODUIT", "CAISSE", "VENTE"
    private Long entiteId;       // ID de l'entité concernée
    private String reference;    // Référence (numéro facture, nom client, etc.)
    private LocalDateTime dateDetection;
    private Map<String, Object> details;

    public AnomalieDTO() {
        this.dateDetection = LocalDateTime.now();
    }

    public AnomalieDTO(String type, String severite, String titre, String message, String entite, Long entiteId, String reference) {
        this.type = type;
        this.severite = severite;
        this.titre = titre;
        this.message = message;
        this.entite = entite;
        this.entiteId = entiteId;
        this.reference = reference;
        this.dateDetection = LocalDateTime.now();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverite() { return severite; }
    public void setSeverite(String severite) { this.severite = severite; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getEntite() { return entite; }
    public void setEntite(String entite) { this.entite = entite; }

    public Long getEntiteId() { return entiteId; }
    public void setEntiteId(Long entiteId) { this.entiteId = entiteId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDateTime getDateDetection() { return dateDetection; }
    public void setDateDetection(LocalDateTime dateDetection) { this.dateDetection = dateDetection; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}
