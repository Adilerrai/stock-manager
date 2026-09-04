package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeNotificationAlerte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AlerteNotificationDTO {
    private String id;
    private TypeNotificationAlerte type;
    private String titre;
    private String message;
    private String niveau; // "INFO", "WARNING", "DANGER"
    private LocalDateTime date;
    private String lien;
    private String reference;
    private BigDecimal montant;

    public AlerteNotificationDTO() {}

    public AlerteNotificationDTO(String id, TypeNotificationAlerte type, String titre, String message,
                                String niveau, LocalDateTime date, String lien, String reference, BigDecimal montant) {
        this.id = id;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.niveau = niveau;
        this.date = date;
        this.lien = lien;
        this.reference = reference;
        this.montant = montant;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public TypeNotificationAlerte getType() { return type; }
    public void setType(TypeNotificationAlerte type) { this.type = type; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getLien() { return lien; }
    public void setLien(String lien) { this.lien = lien; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}
