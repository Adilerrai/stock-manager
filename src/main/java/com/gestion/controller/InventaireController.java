package com.gestion.controller;

import com.gestion.persistent.model.Inventaire;
import com.gestion.persistent.model.LigneInventaire;
import com.gestion.service.InventaireService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventaires")
@CrossOrigin(origins = "*")
public class InventaireController {

    private final InventaireService inventaireService;

    public InventaireController(InventaireService inventaireService) {
        this.inventaireService = inventaireService;
    }

    @PostMapping("/demarrer")
    public ResponseEntity<Inventaire> demarrerInventaire(@RequestParam Long depotId,
                                                         @RequestParam(required = false) String notes,
                                                         @RequestParam(required = false) Long userId) {
        Inventaire inv = inventaireService.creerInventaire(depotId, notes, userId);
        return new ResponseEntity<>(inv, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/lignes")
    public ResponseEntity<Inventaire> mettreAJourLignes(@PathVariable Long id,
                                                        @RequestBody List<LigneInventaire> lignes) {
        Inventaire inv = inventaireService.mettreAJourLignes(id, lignes);
        return ResponseEntity.ok(inv);
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<Inventaire> validerInventaire(@PathVariable Long id,
                                                        @RequestParam(required = false) Long userId) {
        Inventaire valide = inventaireService.validerInventaire(id, userId);
        return ResponseEntity.ok(valide);
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<Void> annulerInventaire(@PathVariable Long id) {
        inventaireService.annulerInventaire(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Inventaire> getInventaireById(@PathVariable Long id) {
        return ResponseEntity.ok(inventaireService.getInventaireById(id));
    }

    @GetMapping
    public ResponseEntity<List<Inventaire>> getAllInventaires() {
        return ResponseEntity.ok(inventaireService.getAllInventaires());
    }

    @GetMapping("/depot/{depotId}")
    public ResponseEntity<List<Inventaire>> getInventairesByDepot(@PathVariable Long depotId) {
        return ResponseEntity.ok(inventaireService.getInventairesByDepot(depotId));
    }
}
