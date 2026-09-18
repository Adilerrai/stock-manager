package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class DocumentComptableDTO {

    private Long id;
    private Long pointDeVenteId;
    private String nomOriginal;
    private String nomStocke;
    private String contentType;
    private Long tailleOctets;
    private String tailleLisible; // ex: "1.2 MB"
    private String sha256Hash;
    private String typePiece;
    private String description;
    private Long ecritureId;
    private Long factureAchatId;
    private Long factureVenteId;
    private Long paiementId;
    private String uploadedBy;
    private LocalDateTime dateUpload;
    private String urlVisualisation;
    private String urlTelechargement;

    public DocumentComptableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getNomOriginal() { return nomOriginal; }
    public void setNomOriginal(String nomOriginal) { this.nomOriginal = nomOriginal; }

    public String getNomStocke() { return nomStocke; }
    public void setNomStocke(String nomStocke) { this.nomStocke = nomStocke; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getTailleOctets() { return tailleOctets; }
    public void setTailleOctets(Long tailleOctets) { this.tailleOctets = tailleOctets; }

    public String getTailleLisible() { return tailleLisible; }
    public void setTailleLisible(String tailleLisible) { this.tailleLisible = tailleLisible; }

    public String getSha256Hash() { return sha256Hash; }
    public void setSha256Hash(String sha256Hash) { this.sha256Hash = sha256Hash; }

    public String getTypePiece() { return typePiece; }
    public void setTypePiece(String typePiece) { this.typePiece = typePiece; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getEcritureId() { return ecritureId; }
    public void setEcritureId(Long ecritureId) { this.ecritureId = ecritureId; }

    public Long getFactureAchatId() { return factureAchatId; }
    public void setFactureAchatId(Long factureAchatId) { this.factureAchatId = factureAchatId; }

    public Long getFactureVenteId() { return factureVenteId; }
    public void setFactureVenteId(Long factureVenteId) { this.factureVenteId = factureVenteId; }

    public Long getPaiementId() { return paiementId; }
    public void setPaiementId(Long paiementId) { this.paiementId = paiementId; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public LocalDateTime getDateUpload() { return dateUpload; }
    public void setDateUpload(LocalDateTime dateUpload) { this.dateUpload = dateUpload; }

    public String getUrlVisualisation() { return urlVisualisation; }
    public void setUrlVisualisation(String urlVisualisation) { this.urlVisualisation = urlVisualisation; }

    public String getUrlTelechargement() { return urlTelechargement; }
    public void setUrlTelechargement(String urlTelechargement) { this.urlTelechargement = urlTelechargement; }
}
