package com.gestion.persistent.model;

import com.gestion.persistent.enums.TypeActionLivreur;
import com.gestion.persistent.enums.TypeDocumentLivreur;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "preuves_depot_livreur", indexes = {
    @Index(name = "idx_preuve_livreur_tenant", columnList = "point_de_vente_id"),
    @Index(name = "idx_preuve_livreur_doc", columnList = "document_type, document_id")
})
public class PreuveDepotLivreur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private TypeDocumentLivreur documentType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "numero_document", nullable = false)
    private String numeroDocument;

    @Column(name = "client_nom")
    private String clientNom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_action", nullable = false)
    private TypeActionLivreur typeAction;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "nom_fichier_photo")
    private String nomFichierPhoto;

    @Column(name = "commentaire", length = 1000)
    private String commentaire;

    @Column(name = "nom_livreur")
    private String nomLivreur;

    @Column(name = "livreur_user_id")
    private Long livreurUserId;

    @Column(name = "date_declaration", nullable = false)
    private LocalDateTime dateDeclaration = LocalDateTime.now();

    private Double latitude;
    private Double longitude;

    public PreuveDepotLivreur() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public TypeDocumentLivreur getDocumentType() { return documentType; }
    public void setDocumentType(TypeDocumentLivreur documentType) { this.documentType = documentType; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getNumeroDocument() { return numeroDocument; }
    public void setNumeroDocument(String numeroDocument) { this.numeroDocument = numeroDocument; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public TypeActionLivreur getTypeAction() { return typeAction; }
    public void setTypeAction(TypeActionLivreur typeAction) { this.typeAction = typeAction; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getNomFichierPhoto() { return nomFichierPhoto; }
    public void setNomFichierPhoto(String nomFichierPhoto) { this.nomFichierPhoto = nomFichierPhoto; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getNomLivreur() { return nomLivreur; }
    public void setNomLivreur(String nomLivreur) { this.nomLivreur = nomLivreur; }

    public Long getLivreurUserId() { return livreurUserId; }
    public void setLivreurUserId(Long livreurUserId) { this.livreurUserId = livreurUserId; }

    public LocalDateTime getDateDeclaration() { return dateDeclaration; }
    public void setDateDeclaration(LocalDateTime dateDeclaration) { this.dateDeclaration = dateDeclaration; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
