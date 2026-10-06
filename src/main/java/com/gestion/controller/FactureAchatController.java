package com.gestion.controller;

import com.gestion.persistent.dto.FactureAchatDTO;
import com.gestion.service.FactureAchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/factures-achat")
@CrossOrigin(origins = "*")
public class FactureAchatController {

    private final FactureAchatService factureAchatService;
    private final com.gestion.service.ImpressionService impressionService;

    public FactureAchatController(FactureAchatService factureAchatService,
                                  com.gestion.service.ImpressionService impressionService) {
        this.factureAchatService = factureAchatService;
        this.impressionService = impressionService;
    }

    @PostMapping
    public ResponseEntity<FactureAchatDTO> creerFactureAchat(@RequestBody FactureAchatDTO dto) {
        return ResponseEntity.ok(factureAchatService.creerFactureAchat(dto));
    }

    @GetMapping
    public ResponseEntity<List<FactureAchatDTO>> getFacturesAchat() {
        return ResponseEntity.ok(factureAchatService.getFacturesAchat());
    }

    @GetMapping("/impayees")
    public ResponseEntity<List<FactureAchatDTO>> getFacturesAchatImpayees() {
        return ResponseEntity.ok(factureAchatService.getFacturesAchatImpayees());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureAchatDTO> getFactureAchatById(@PathVariable Long id) {
        return ResponseEntity.ok(factureAchatService.getFactureAchatById(id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> imprimerFactureAchatPdf(@PathVariable Long id) {
        byte[] pdf = impressionService.genererFactureAchatPdf(id);
        return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"facture_achat_" + id + ".pdf\"")
                .body(pdf);
    }
}
