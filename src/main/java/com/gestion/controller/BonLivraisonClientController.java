package com.gestion.controller;

import com.gestion.mapper.BonLivraisonClientMapper;
import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.persistent.dto.BonLivraisonClientSearchCriteria;
import com.gestion.persistent.model.BonLivraisonClient;
import com.gestion.service.BonLivraisonClientService;
import com.gestion.service.LivraisonRetardService;
import com.gestion.persistent.dto.LivraisonRetardDTO;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.gestion.persistent.dto.DocumentComptableDTO;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/livraisons-client", "/api/bons-livraison-clients"})
@CrossOrigin(origins = "*")
public class BonLivraisonClientController {

    private final BonLivraisonClientService bonLivraisonClientService;
    private final com.gestion.service.ImpressionService impressionService;
    private final BonLivraisonClientMapper bonLivraisonClientMapper;
    private final LivraisonRetardService livraisonRetardService;

    public BonLivraisonClientController(BonLivraisonClientService bonLivraisonClientService,
                                        com.gestion.service.ImpressionService impressionService,
                                        BonLivraisonClientMapper bonLivraisonClientMapper,
                                        LivraisonRetardService livraisonRetardService) {
        this.bonLivraisonClientService = bonLivraisonClientService;
        this.impressionService = impressionService;
        this.bonLivraisonClientMapper = bonLivraisonClientMapper;
        this.livraisonRetardService = livraisonRetardService;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<BonLivraisonClientDTO>> searchBonsLivraison(
            @RequestBody BonLivraisonClientSearchCriteria criteria, Pageable pageable) {
        Page<BonLivraisonClient> page = bonLivraisonClientService.searchBonsLivraison(criteria, pageable);
        return ResponseEntity.ok(page.map(bonLivraisonClientMapper::toDto));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getBonLivraisonPdf(@PathVariable Long id) {
        byte[] pdf = impressionService.genererBonLivraisonPdf(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bon-livraison-" + id + ".pdf\"")
                .body(pdf);
    }

    @PostMapping
    public ResponseEntity<BonLivraisonClientDTO> creerBonLivraisonClient(@RequestBody BonLivraisonClientDTO blDto) {
        return ResponseEntity.ok(bonLivraisonClientService.creerBonLivraisonClient(blDto));
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<BonLivraisonClientDTO> validerEtExpedierBL(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.validerEtExpedierBL(id));
    }


    @PostMapping("/{id}/brouillon")
    public ResponseEntity<BonLivraisonClientDTO> remettreEnBrouillon(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.remettreEnBrouillon(id));
    }

    @PutMapping("/{id}/brouillon")
    public ResponseEntity<BonLivraisonClientDTO> remettreEnBrouillonPut(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.remettreEnBrouillon(id));
    }

    @RequestMapping(value = "/{id}/statut", method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<BonLivraisonClientDTO> changerStatut(
            @PathVariable Long id,
            @RequestParam(value = "statut", required = false) String statut,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        String effectiveStatut = statut;
        if ((effectiveStatut == null || effectiveStatut.isBlank()) && body != null) {
            effectiveStatut = body.get("statut");
        }
        if (effectiveStatut != null && "BROUILLON".equalsIgnoreCase(effectiveStatut.trim())) {
            return ResponseEntity.ok(bonLivraisonClientService.remettreEnBrouillon(id));
        } else if (effectiveStatut != null && ("LIVREE".equalsIgnoreCase(effectiveStatut.trim()) || "VALIDEE".equalsIgnoreCase(effectiveStatut.trim()))) {
            return ResponseEntity.ok(bonLivraisonClientService.validerEtExpedierBL(id));
        } else if (effectiveStatut != null && "ANNULEE".equalsIgnoreCase(effectiveStatut.trim())) {
            return ResponseEntity.ok(bonLivraisonClientService.annulerBonLivraisonClient(id));
        }
        return ResponseEntity.ok(bonLivraisonClientService.getBonLivraisonById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<BonLivraisonClientDTO> updateBonLivraisonClient(@PathVariable Long id, @RequestBody BonLivraisonClientDTO dto) {
        return ResponseEntity.ok(bonLivraisonClientService.updateBonLivraisonClient(id, dto));
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<BonLivraisonClientDTO> annulerBonLivraisonClient(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.annulerBonLivraisonClient(id));
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<BonLivraisonClientDTO> annulerBonLivraisonClientPut(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.annulerBonLivraisonClient(id));
    }

    @GetMapping
    public ResponseEntity<List<BonLivraisonClientDTO>> getBonsLivraison() {
        return ResponseEntity.ok(bonLivraisonClientService.getBonsLivraison());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonLivraisonClientDTO> getBonLivraisonById(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.getBonLivraisonById(id));
    }

    @GetMapping("/non-factures")
    public ResponseEntity<List<BonLivraisonClientDTO>> getBonsLivraisonNonFactures(@RequestParam(required = false) Long clientId) {
        return ResponseEntity.ok(bonLivraisonClientService.getBonsLivraisonNonFactures(clientId));
    }

    @GetMapping("/retards")
    public ResponseEntity<List<LivraisonRetardDTO>> getRetardsClients(
            @RequestParam(value = "pointDeVenteId", required = false) Long pointDeVenteId) {
        return ResponseEntity.ok(livraisonRetardService.getRetardsClients(pointDeVenteId));
    }

    @GetMapping("/retards/export-excel")
    public ResponseEntity<byte[]> exporterExcelRetardsClients(
            @RequestParam(value = "pointDeVenteId", required = false) Long pointDeVenteId) {
        byte[] bytes = livraisonRetardService.genererExcelRetardsClients(pointDeVenteId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Livraisons_Clients_En_Retard.xlsx")
                .body(bytes);
    }

    // =========================================================================
    // PIÈCES JOINTES & JUSTIFICATIFS (GED) DU BON DE LIVRAISON CLIENT
    // =========================================================================

    @PostMapping(value = "/{id}/pieces-jointes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentComptableDTO> ajouterPieceJointe(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "typePiece", required = false, defaultValue = "BON_LIVRAISON_CLIENT") String typePiece) throws IOException {
        DocumentComptableDTO piece = bonLivraisonClientService.ajouterPieceJointe(id, file, description, typePiece);
        return ResponseEntity.status(HttpStatus.CREATED).body(piece);
    }

    @PostMapping(value = "/{id}/pieces-jointes/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentComptableDTO>> ajouterPiecesJointesMultiples(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "typePiece", required = false, defaultValue = "BON_LIVRAISON_CLIENT") String typePiece) throws IOException {
        List<DocumentComptableDTO> pieces = bonLivraisonClientService.ajouterPiecesJointes(id, files, description, typePiece);
        return ResponseEntity.status(HttpStatus.CREATED).body(pieces);
    }

    @GetMapping("/{id}/pieces-jointes")
    public ResponseEntity<List<DocumentComptableDTO>> getPiecesJointes(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.getPiecesJointes(id));
    }

    @DeleteMapping("/{id}/pieces-jointes/{pieceJointeId}")
    public ResponseEntity<Void> supprimerPieceJointe(@PathVariable Long id, @PathVariable Long pieceJointeId) {
        bonLivraisonClientService.supprimerPieceJointe(id, pieceJointeId);
        return ResponseEntity.noContent().build();
    }

}
