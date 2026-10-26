package com.gestion.controller;

import com.gestion.persistent.dto.CodificationDocumentDTO;
import com.gestion.persistent.enums.TypeDocumentCodification;
import com.gestion.service.CodificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/codifications")
@CrossOrigin(origins = "*")
public class CodificationController {

    private final CodificationService codificationService;

    public CodificationController(CodificationService codificationService) {
        this.codificationService = codificationService;
    }

    @GetMapping
    public ResponseEntity<List<CodificationDocumentDTO>> getCodifications() {
        return ResponseEntity.ok(codificationService.getCodificationsCurrentTenant());
    }

    @PutMapping("/{typeDocument}")
    public ResponseEntity<CodificationDocumentDTO> updateCodification(
            @PathVariable TypeDocumentCodification typeDocument,
            @RequestBody CodificationDocumentDTO dto) {
        return ResponseEntity.ok(codificationService.updateCodification(typeDocument, dto));
    }

    @PostMapping("/reinitialiser")
    public ResponseEntity<List<CodificationDocumentDTO>> reinitialiserDefauts() {
        codificationService.reinitialiserDefautsCurrentTenant();
        return ResponseEntity.ok(codificationService.getCodificationsCurrentTenant());
    }

    @PostMapping("/apercu")
    public ResponseEntity<Map<String, String>> genererApercu(@RequestBody CodificationDocumentDTO dto) {
        String apercu = codificationService.genererApercu(dto);
        return ResponseEntity.ok(Collections.singletonMap("apercu", apercu));
    }

    @GetMapping("/variables")
    public ResponseEntity<Map<String, String>> getVariablesDisponibles() {
        Map<String, String> variables = new java.util.LinkedHashMap<>();
        variables.put("{DEPOT}", "Code du dépôt (ex: TAC, WH, BER)");
        variables.put("{CODE_DEPOT}", "Alias pour le code du dépôt (ex: TAC)");
        variables.put("{PREFIX}", "Préfixe du document (ex: IN, OUT, FACT)");
        variables.put("{AAAA}", "Année sur 4 chiffres (ex: 2026)");
        variables.put("{AA}", "Année sur 2 chiffres (ex: 26)");
        variables.put("{MM}", "Mois sur 2 chiffres (ex: 10)");
        variables.put("{NUM}", "Séquence numérique avec zéros selon longueur (ex: 00319)");
        return ResponseEntity.ok(variables);
    }
}
