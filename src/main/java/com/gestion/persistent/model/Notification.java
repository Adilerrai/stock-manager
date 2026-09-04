package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.SeveriteNotification;
import com.gestion.persistent.enums.TypeNotification;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type = TypeNotification.SYSTEME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeveriteNotification severite = SeveriteNotification.INFO;

    @Column(nullable = false)
    private Boolean lu = false;

    @Column(name = "lien_action")
    private String lienAction;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_lecture")
    private LocalDateTime dateLecture;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Notification() {}

    public Notification(String titre, String message, TypeNotification type, SeveriteNotification severite, String lienAction, Long pointDeVenteId) {
        this.titre = titre;
        this.message = message;
        this.type = type != null ? type : TypeNotification.SYSTEME;
        this.severite = severite != null ? severite : SeveriteNotification.INFO;
        this.lienAction = lienAction;
        this.pointDeVenteId = pointDeVenteId != null ? pointDeVenteId : 1L;
        this.lu = false;
        this.dateCreation = LocalDateTime.now();
    }

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

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
