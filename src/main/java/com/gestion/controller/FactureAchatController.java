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

    public FactureAchatController(FactureAchatService factureAchatService) {
        this.factureAchatService = factureAchatService;
    }

    @PostMapping
    public ResponseEntity<FactureAchatDTO> creerFactureAchat(@RequestBody FactureAchatDTO dto) {
        return ResponseEntity.ok(factureAchatService.creerFactureAchat(dto));
    }

    @GetMapping
    public ResponseEntity<List<FactureAchatDTO>> getFacturesAchat() {
        return ResponseEntity.ok(factureAchatService.getFacturesAchat());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureAchatDTO> getFactureAchatById(@PathVariable Long id) {
        return ResponseEntity.ok(factureAchatService.getFactureAchatById(id));
    }
}
