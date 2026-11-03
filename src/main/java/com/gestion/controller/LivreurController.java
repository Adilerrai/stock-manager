package com.gestion.controller;

import com.gestion.persistent.dto.DocumentLivreurCandidateDTO;
import com.gestion.persistent.enums.TypeActionLivreur;
import com.gestion.persistent.enums.TypeDocumentLivreur;
import com.gestion.persistent.model.PreuveDepotLivreur;
import com.gestion.service.LivreurService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livreur")
@CrossOrigin(origins = "*")
public class LivreurController {

    private final LivreurService livreurService;

    public LivreurController(LivreurService livreurService) {
        this.livreurService = livreurService;
    }

    @GetMapping("/recherche-documents")
    public ResponseEntity<List<DocumentLivreurCandidateDTO>> searchDocuments(
            @RequestParam(value = "query", required = false) String query,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "typeDocument", required = false) String typeDocument,
            @RequestParam(value = "type", required = false) String type) {
        String effectiveQuery = (q != null && !q.isBlank()) ? q : query;
        String effectiveType = (type != null && !type.isBlank()) ? type : (typeDocument != null ? typeDocument : "ALL");
        List<DocumentLivreurCandidateDTO> results = livreurService.searchDocuments(effectiveQuery, effectiveType);
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/declarer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreuveDepotLivreur> declarerAction(
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "documentType", required = false) String documentTypeStr,
            @RequestParam("documentId") Long documentId,
            @RequestParam(value = "numeroDocument", required = false) String numeroDocument,
            @RequestParam(value = "clientNom", required = false) String clientNom,
            @RequestParam("typeAction") String typeActionStr,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            @RequestParam(value = "nomLivreur", required = false) String nomLivreur,
            @RequestParam(value = "livreurUserId", required = false) Long livreurUserId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) throws Exception {

        PreuveDepotLivreur preuve = livreurService.declarerActionFlexible(
                photo, documentTypeStr, documentId, numeroDocument, clientNom,
                typeActionStr, commentaire, nomLivreur, livreurUserId, latitude, longitude
        );

        return ResponseEntity.ok(preuve);
    }

    @GetMapping("/historique")
    public ResponseEntity<List<PreuveDepotLivreur>> getHistorique() {
        return ResponseEntity.ok(livreurService.getHistorique());
    }
}
