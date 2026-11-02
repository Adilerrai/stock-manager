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
            @RequestParam(value = "typeDocument", required = false, defaultValue = "ALL") String typeDocument) {
        List<DocumentLivreurCandidateDTO> results = livreurService.searchDocuments(query, typeDocument);
        return ResponseEntity.ok(results);
    }

    @PostMapping(value = "/declarer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PreuveDepotLivreur> declarerAction(
            @RequestPart(value = "photo", required = false) MultipartFile photo,
            @RequestParam("documentType") TypeDocumentLivreur documentType,
            @RequestParam("documentId") Long documentId,
            @RequestParam("numeroDocument") String numeroDocument,
            @RequestParam(value = "clientNom", required = false) String clientNom,
            @RequestParam("typeAction") TypeActionLivreur typeAction,
            @RequestParam(value = "commentaire", required = false) String commentaire,
            @RequestParam(value = "nomLivreur", required = false) String nomLivreur,
            @RequestParam(value = "livreurUserId", required = false) Long livreurUserId,
            @RequestParam(value = "latitude", required = false) Double latitude,
            @RequestParam(value = "longitude", required = false) Double longitude) throws Exception {

        PreuveDepotLivreur preuve = livreurService.declarerAction(
                photo, documentType, documentId, numeroDocument, clientNom,
                typeAction, commentaire, nomLivreur, livreurUserId, latitude, longitude
        );

        return ResponseEntity.ok(preuve);
    }

    @GetMapping("/historique")
    public ResponseEntity<List<PreuveDepotLivreur>> getHistorique() {
        return ResponseEntity.ok(livreurService.getHistorique());
    }
}
