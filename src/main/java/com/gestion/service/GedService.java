package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.persistent.model.DocumentComptable;
import com.gestion.repository.DocumentComptableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class GedService {

    private final DocumentComptableRepository documentRepository;
    private final AuditService auditService;
    private final Path rootLocation;

    public GedService(DocumentComptableRepository documentRepository,
                      AuditService auditService,
                      @Value("${storage.ged.path:./uploads/ged}") String storagePath) {
        this.documentRepository = documentRepository;
        this.auditService = auditService;
        this.rootLocation = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Impossible d'initialiser le répertoire racine de la GED: " + this.rootLocation, e);
        }
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    private String getUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                if (auth.getPrincipal() instanceof User u) {
                    return u.getUsername() != null ? u.getUsername() : u.getEmail();
                }
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "system";
    }

    public DocumentComptableDTO stockerDocument(
            MultipartFile file,
            String typePiece,
            String description,
            Long ecritureId,
            Long factureAchatId,
            Long factureVenteId,
            Long paiementId) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier à téléverser ne peut pas être vide.");
        }

        Long tenantId = getTenantId();
        String nomOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document_" + System.currentTimeMillis();
        
        // Nettoyage nom de fichier
        nomOriginal = Paths.get(nomOriginal).getFileName().toString();

        String extension = "";
        int dotIdx = nomOriginal.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = nomOriginal.substring(dotIdx);
        }

        String nomStocke = UUID.randomUUID() + extension;

        // Répertoire par tenant : uploads/ged/{tenantId}/
        Path tenantFolder = this.rootLocation.resolve(String.valueOf(tenantId)).normalize();
        Files.createDirectories(tenantFolder);

        Path destinationPath = tenantFolder.resolve(nomStocke).normalize();

        // Calcul du hash SHA-256 de la pièce justificative
        String sha256Hash = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
                sha256Hash = HexFormat.of().formatHex(digest.digest());
            }
        } catch (NoSuchAlgorithmException e) {
            sha256Hash = "SHA256_UNAVAILABLE";
        }

        // Sauvegarde physique sur disque
        try (InputStream is = file.getInputStream()) {
            Files.copy(is, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Création de l'entité DocumentComptable
        DocumentComptable doc = new DocumentComptable();
        doc.setPointDeVenteId(tenantId);
        doc.setNomOriginal(nomOriginal);
        doc.setNomStocke(nomStocke);
        doc.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        doc.setTailleOctets(file.getSize());
        doc.setCheminStockage(destinationPath.toString());
        doc.setSha256Hash(sha256Hash);
        doc.setTypePiece(typePiece != null && !typePiece.isBlank() ? typePiece.toUpperCase() : "AUTRE");
        doc.setDescription(description);
        doc.setEcritureId(ecritureId);
        doc.setFactureAchatId(factureAchatId);
        doc.setFactureVenteId(factureVenteId);
        doc.setPaiementId(paiementId);
        doc.setUploadedBy(getUsername());
        doc.setDateUpload(LocalDateTime.now());

        DocumentComptable saved = documentRepository.save(doc);

        auditService.logCreation("DOCUMENT_COMPTABLE", saved.getId(), 
                "GED: Ajout pièce " + saved.getNomOriginal() + " (Type: " + saved.getTypePiece() 
                + ", SHA-256: " + saved.getSha256Hash() + ")");

        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentComptableDTO> listerDocuments() {
        Long tenantId = getTenantId();
        return documentRepository.findByPointDeVenteIdOrderByDateUploadDesc(tenantId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentComptableDTO> getDocumentsParEcriture(Long ecritureId) {
        Long tenantId = getTenantId();
        return documentRepository.findByPointDeVenteIdAndEcritureIdOrderByDateUploadDesc(tenantId, ecritureId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentComptableDTO> getDocumentsParFactureAchat(Long factureAchatId) {
        Long tenantId = getTenantId();
        return documentRepository.findByPointDeVenteIdAndFactureAchatIdOrderByDateUploadDesc(tenantId, factureAchatId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentComptableDTO> getDocumentsParFactureVente(Long factureVenteId) {
        Long tenantId = getTenantId();
        return documentRepository.findByPointDeVenteIdAndFactureVenteIdOrderByDateUploadDesc(tenantId, factureVenteId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentComptableDTO> getDocumentsParPaiement(Long paiementId) {
        Long tenantId = getTenantId();
        return documentRepository.findByPointDeVenteIdAndPaiementIdOrderByDateUploadDesc(tenantId, paiementId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentComptableDTO getDocument(Long id) {
        Long tenantId = getTenantId();
        DocumentComptable doc = documentRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));
        return toDto(doc);
    }

    @Transactional(readOnly = true)
    public Resource chargerFichier(Long id) {
        Long tenantId = getTenantId();
        DocumentComptable doc = documentRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable avec l'ID: " + id));

        try {
            Path file = Paths.get(doc.getCheminStockage());
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Fichier non lisible sur le serveur: " + doc.getNomOriginal());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur de chemin de fichier pour le document " + id, e);
        }
    }

    public DocumentComptableDTO lierAEcriture(Long documentId, Long ecritureId) {
        Long tenantId = getTenantId();
        DocumentComptable doc = documentRepository.findByIdAndPointDeVenteId(documentId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable ID: " + documentId));

        doc.setEcritureId(ecritureId);
        DocumentComptable saved = documentRepository.save(doc);

        auditService.logModification("DOCUMENT_COMPTABLE", saved.getId(), "ecritureId", 
                "", String.valueOf(ecritureId), "Liaison de la pièce " + saved.getNomOriginal() + " à l'écriture " + ecritureId);

        return toDto(saved);
    }

    public void supprimerDocument(Long id) {
        Long tenantId = getTenantId();
        DocumentComptable doc = documentRepository.findByIdAndPointDeVenteId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Document introuvable ID: " + id));

        // Suppression physique du fichier sur le disque
        try {
            Path path = Paths.get(doc.getCheminStockage());
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Logger l'erreur sans bloquer la suppression de la ligne
        }

        documentRepository.delete(doc);
        auditService.logSuppression("DOCUMENT_COMPTABLE", id, "GED: Suppression pièce " + doc.getNomOriginal());
    }

    private DocumentComptableDTO toDto(DocumentComptable doc) {
        DocumentComptableDTO dto = new DocumentComptableDTO();
        dto.setId(doc.getId());
        dto.setPointDeVenteId(doc.getPointDeVenteId());
        dto.setNomOriginal(doc.getNomOriginal());
        dto.setNomStocke(doc.getNomStocke());
        dto.setContentType(doc.getContentType());
        dto.setTailleOctets(doc.getTailleOctets());
        dto.setTailleLisible(formaterTaille(doc.getTailleOctets()));
        dto.setSha256Hash(doc.getSha256Hash());
        dto.setTypePiece(doc.getTypePiece());
        dto.setDescription(doc.getDescription());
        dto.setEcritureId(doc.getEcritureId());
        dto.setFactureAchatId(doc.getFactureAchatId());
        dto.setFactureVenteId(doc.getFactureVenteId());
        dto.setPaiementId(doc.getPaiementId());
        dto.setUploadedBy(doc.getUploadedBy());
        dto.setDateUpload(doc.getDateUpload());
        dto.setUrlVisualisation("/api/v1/ged/documents/" + doc.getId() + "/visualiser");
        dto.setUrlTelechargement("/api/v1/ged/documents/" + doc.getId() + "/telecharger");
        return dto;
    }

    private String formaterTaille(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }
}
