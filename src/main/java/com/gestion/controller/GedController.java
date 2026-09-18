package com.gestion.controller;

import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.service.GedService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ged")
@CrossOrigin(origins = "*")
public class GedController {

    private final GedService gedService;

    public GedController(GedService gedService) {
        this.gedService = gedService;
    }

    // =========================================================================
    // UPLOAD & ENREGISTREMENT PIÈCE JUSTIFICATIVE
    // =========================================================================

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentComptableDTO> uploaderDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "typePiece", required = false, defaultValue = "AUTRE") String typePiece,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "ecritureId", required = false) Long ecritureId,
            @RequestParam(value = "factureAchatId", required = false) Long factureAchatId,
            @RequestParam(value = "factureVenteId", required = false) Long factureVenteId,
            @RequestParam(value = "paiementId", required = false) Long paiementId) throws IOException {

        DocumentComptableDTO dto = gedService.stockerDocument(
                file, typePiece, description, ecritureId, factureAchatId, factureVenteId, paiementId
        );
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    // =========================================================================
    // CONSULTATION & RECHERCHE
    // =========================================================================

    @GetMapping("/documents")
    public ResponseEntity<List<DocumentComptableDTO>> listerDocuments() {
        return ResponseEntity.ok(gedService.listerDocuments());
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentComptableDTO> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(gedService.getDocument(id));
    }

    @GetMapping("/documents/ecriture/{ecritureId}")
    public ResponseEntity<List<DocumentComptableDTO>> getDocumentsParEcriture(@PathVariable Long ecritureId) {
        return ResponseEntity.ok(gedService.getDocumentsParEcriture(ecritureId));
    }

    @GetMapping("/documents/facture-achat/{factureAchatId}")
    public ResponseEntity<List<DocumentComptableDTO>> getDocumentsParFactureAchat(@PathVariable Long factureAchatId) {
        return ResponseEntity.ok(gedService.getDocumentsParFactureAchat(factureAchatId));
    }

    @GetMapping("/documents/facture-vente/{factureVenteId}")
    public ResponseEntity<List<DocumentComptableDTO>> getDocumentsParFactureVente(@PathVariable Long factureVenteId) {
        return ResponseEntity.ok(gedService.getDocumentsParFactureVente(factureVenteId));
    }

    @GetMapping("/documents/paiement/{paiementId}")
    public ResponseEntity<List<DocumentComptableDTO>> getDocumentsParPaiement(@PathVariable Long paiementId) {
        return ResponseEntity.ok(gedService.getDocumentsParPaiement(paiementId));
    }

    // =========================================================================
    // VISUALISATION INLINE & TÉLÉCHARGEMENT
    // =========================================================================

    @GetMapping("/documents/{id}/visualiser")
    public ResponseEntity<Resource> visualiserDocument(@PathVariable Long id) {
        DocumentComptableDTO doc = gedService.getDocument(id);
        Resource file = gedService.chargerFichier(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(doc.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getNomOriginal() + "\"")
                .body(file);
    }

    @GetMapping("/documents/{id}/telecharger")
    public ResponseEntity<Resource> telechargerDocument(@PathVariable Long id) {
        DocumentComptableDTO doc = gedService.getDocument(id);
        Resource file = gedService.chargerFichier(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNomOriginal() + "\"")
                .body(file);
    }

    // =========================================================================
    // LIAISON A POSTERIORI & SUPPRESSION
    // =========================================================================

    @PostMapping("/documents/{id}/lier-ecriture/{ecritureId}")
    public ResponseEntity<DocumentComptableDTO> lierAEcriture(
            @PathVariable Long id,
            @PathVariable Long ecritureId) {
        return ResponseEntity.ok(gedService.lierAEcriture(id, ecritureId));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> supprimerDocument(@PathVariable Long id) {
        gedService.supprimerDocument(id);
        return ResponseEntity.noContent().build();
    }
}
