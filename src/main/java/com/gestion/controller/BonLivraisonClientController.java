package com.gestion.controller;

import com.gestion.persistent.dto.BonLivraisonClientDTO;
import com.gestion.service.BonLivraisonClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livraisons-client")
@CrossOrigin(origins = "*")
public class BonLivraisonClientController {

    private final BonLivraisonClientService bonLivraisonClientService;
    private final com.gestion.service.ImpressionService impressionService;

    public BonLivraisonClientController(BonLivraisonClientService bonLivraisonClientService,
                                        com.gestion.service.ImpressionService impressionService) {
        this.bonLivraisonClientService = bonLivraisonClientService;
        this.impressionService = impressionService;
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
}
