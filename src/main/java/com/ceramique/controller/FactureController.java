package com.ceramique.controller;

import com.ceramique.persistent.model.Facture;
import com.ceramique.service.FactureService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/factures")
@CrossOrigin(origins = "*")
public class FactureController {

    private final FactureService factureService;

    public FactureController(FactureService factureService) {
        this.factureService = factureService;
    }

    @PostMapping
    public ResponseEntity<Facture> creerFacture(@RequestBody Facture facture, @RequestParam Long userId) {
        Facture nouvelleFacture = factureService.creerFacture(facture, userId);
        return new ResponseEntity<>(nouvelleFacture, HttpStatus.CREATED);
    }

    @PostMapping("/depuis-vente/{venteId}")
    public ResponseEntity<Facture> creerFactureDepuisVente(@PathVariable Long venteId, @RequestParam Long userId) {
        // Cette méthode nécessite d'injecter VenteService dans FactureService ou de passer la vente
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Facture> getFacture(@PathVariable Long id) {
        Facture facture = factureService.getFactureById(id);
        return ResponseEntity.ok(facture);
    }

    @GetMapping
    public ResponseEntity<List<Facture>> getAllFactures() {
        List<Facture> factures = factureService.getAllFactures();
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Facture>> getFacturesByClient(@PathVariable Long clientId) {
        List<Facture> factures = factureService.getFacturesByClient(clientId);
        return ResponseEntity.ok(factures);
    }



    @GetMapping("/impayees")
    public ResponseEntity<List<Facture>> getFacturesImpayees() {
        List<Facture> factures = factureService.getFacturesImpayees();
        return ResponseEntity.ok(factures);
    }

    @GetMapping("/echues")
    public ResponseEntity<List<Facture>> getFacturesEchues() {
        List<Facture> factures = factureService.getFacturesEchues();
        return ResponseEntity.ok(factures);
    }

    @PostMapping("/{factureId}/valider")
    public ResponseEntity<Facture> validerFacture(@PathVariable Long factureId) {
        Facture facture = factureService.validerFacture(factureId);
        return ResponseEntity.ok(facture);
    }

    @PostMapping("/{factureId}/annuler")
    public ResponseEntity<Facture> annulerFacture(@PathVariable Long factureId,
                                                   @RequestParam String motif,
                                                   @RequestParam Long userId) {
        Facture facture = factureService.annulerFacture(factureId, motif, userId);
        return ResponseEntity.ok(facture);
    }
}

