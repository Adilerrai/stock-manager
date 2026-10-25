package com.gestion.controller;

import com.gestion.persistent.dto.ReglementFournisseurSearchCriteria;
import com.gestion.persistent.model.ReglementFournisseur;
import com.gestion.service.ReglementFournisseurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reglements-fournisseur")
@CrossOrigin(origins = "*")
public class ReglementFournisseurController {

    private final ReglementFournisseurService reglementFournisseurService;

    public ReglementFournisseurController(ReglementFournisseurService reglementFournisseurService) {
        this.reglementFournisseurService = reglementFournisseurService;
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ReglementFournisseur>> searchReglements(
            @RequestBody(required = false) ReglementFournisseurSearchCriteria criteria,
            Pageable pageable) {
        return ResponseEntity.ok(reglementFournisseurService.searchReglements(criteria, pageable));
    }

    @PostMapping
    public ResponseEntity<ReglementFournisseur> enregistrerReglement(@RequestBody ReglementFournisseur reglement) {
        return ResponseEntity.ok(reglementFournisseurService.enregistrerReglement(reglement));
    }

    @GetMapping
    public ResponseEntity<List<ReglementFournisseur>> getReglements() {
        return ResponseEntity.ok(reglementFournisseurService.getReglements());
    }

    @GetMapping("/facture/{factureId}")
    public ResponseEntity<List<ReglementFournisseur>> getReglementsByFacture(@PathVariable Long factureId) {
        return ResponseEntity.ok(reglementFournisseurService.getReglementsByFacture(factureId));
    }
}

