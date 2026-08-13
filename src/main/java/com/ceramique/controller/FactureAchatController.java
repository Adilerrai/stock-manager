package com.ceramique.controller;

import com.ceramique.persistent.model.FactureAchat;
import com.ceramique.service.FactureAchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/factures-achat")
public class FactureAchatController {

    private final FactureAchatService factureAchatService;

    public FactureAchatController(FactureAchatService factureAchatService) {
        this.factureAchatService = factureAchatService;
    }

    @PostMapping
    public ResponseEntity<FactureAchat> creerFactureAchat(@RequestBody FactureAchat facture) {
        return ResponseEntity.ok(factureAchatService.creerFactureAchat(facture));
    }

    @GetMapping
    public ResponseEntity<List<FactureAchat>> getFacturesAchat() {
        return ResponseEntity.ok(factureAchatService.getFacturesAchat());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FactureAchat> getFactureAchatById(@PathVariable Long id) {
        return ResponseEntity.ok(factureAchatService.getFactureAchatById(id));
    }
}
