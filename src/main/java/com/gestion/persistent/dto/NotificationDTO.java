package com.gestion.persistent.dto;

import com.gestion.persistent.enums.SeveriteNotification;
import com.gestion.persistent.enums.TypeNotification;

import java.time.LocalDateTime;

public class NotificationDTO {
    private Long id;
    private String titre;
    private String message;
    private TypeNotification type;
    private SeveriteNotification severite;
    private Boolean lu;
    private String lienAction;
    private LocalDateTime dateCreation;

    public NotificationDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public TypeNotification getType() { return type; }
    public void setType(TypeNotification type) { this.type = type; }

    public SeveriteNotification getSeverite() { return severite; }
    public void setSeverite(SeveriteNotification severite) { this.severite = severite; }

    public Boolean getLu() { return lu; }
    public void setLu(Boolean lu) { this.lu = lu; }

    public String getLienAction() { return lienAction; }
    public void setLienAction(String lienAction) { this.lienAction = lienAction; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
