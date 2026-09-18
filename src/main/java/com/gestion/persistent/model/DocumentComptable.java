package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents_comptables")
public class DocumentComptable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId; // Tenant / Société active

    @Column(name = "nom_original", nullable = false, length = 255)
    private String nomOriginal;

    @Column(name = "nom_stocke", nullable = false, length = 255, unique = true)
    private String nomStocke;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "taille_octets")
    private Long tailleOctets;

    @Column(name = "chemin_stockage", length = 500)
    private String cheminStockage;

    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash; // Empreinte d'intégrité probante

    @Column(name = "type_piece", length = 50)
    private String typePiece; // FACTURE_ACHAT, FACTURE_VENTE, RECU_PAIEMENT, RELEVE_BANCAIRE, CONTRAT, DECLARATION_FISCALE, AUTRE

    @Column(name = "description", length = 500)
    private String description;

    // Liaisons directes
    @Column(name = "ecriture_id")
    private Long ecritureId;

    @Column(name = "facture_achat_id")
    private Long factureAchatId;

    @Column(name = "facture_vente_id")
    private Long factureVenteId;

    @Column(name = "paiement_id")
    private Long paiementId;

    @Column(name = "uploaded_by", length = 150)
    private String uploadedBy;

    @Column(name = "date_upload")
    private LocalDateTime dateUpload = LocalDateTime.now();

    public DocumentComptable() {}

    @PrePersist
    public void prePersist() {
        if (this.dateUpload == null) {
            this.dateUpload = LocalDateTime.now();
        }
    }

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

    public String getCheminStockage() { return cheminStockage; }
    public void setCheminStockage(String cheminStockage) { this.cheminStockage = cheminStockage; }

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
}
