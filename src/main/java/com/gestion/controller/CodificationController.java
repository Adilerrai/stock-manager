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
}
