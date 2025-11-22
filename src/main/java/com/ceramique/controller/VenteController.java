package com.ceramique.controller;

import com.ceramique.persistent.model.LigneVente;
import com.ceramique.persistent.model.Vente;
import com.ceramique.service.VenteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ventes")
@CrossOrigin(origins = "*")
public class VenteController {

    private final VenteService venteService;

    public VenteController(VenteService venteService) {
        this.venteService = venteService;
    }

    @PostMapping
    public ResponseEntity<Vente> creerVente(@RequestBody Vente vente, @RequestParam Long vendeurId) {
        Vente nouvelleVente = venteService.creerVente(vente, vendeurId);
        return new ResponseEntity<>(nouvelleVente, HttpStatus.CREATED);
    }

    @PostMapping("/{venteId}/lignes")
    public ResponseEntity<Vente> ajouterLigne(@PathVariable Long venteId, @RequestBody LigneVente ligne) {
        Vente vente = venteService.ajouterLigneVente(venteId, ligne);
        return ResponseEntity.ok(vente);
    }

    @DeleteMapping("/{venteId}/lignes/{ligneId}")
    public ResponseEntity<Vente> supprimerLigne(@PathVariable Long venteId, @PathVariable Long ligneId) {
        Vente vente = venteService.supprimerLigneVente(venteId, ligneId);
        return ResponseEntity.ok(vente);
    }

    @PostMapping("/{venteId}/valider")
    public ResponseEntity<Vente> validerVente(@PathVariable Long venteId) {
        Vente vente = venteService.validerVente(venteId);
        return ResponseEntity.ok(vente);
    }

    @PostMapping("/{venteId}/annuler")
    public ResponseEntity<Vente> annulerVente(@PathVariable Long venteId,
                                               @RequestParam String motif,
                                               @RequestParam Long userId) {
        Vente vente = venteService.annulerVente(venteId, motif, userId);
        return ResponseEntity.ok(vente);
    }

    @PatchMapping("/{venteId}/remise")
    public ResponseEntity<Vente> appliquerRemise(@PathVariable Long venteId, @RequestParam BigDecimal remise) {
        Vente vente = venteService.appliquerRemiseGlobale(venteId, remise);
        return ResponseEntity.ok(vente);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vente> getVente(@PathVariable Long id) {
        Vente vente = venteService.getVenteById(id);
        return ResponseEntity.ok(vente);
    }

    @GetMapping
    public ResponseEntity<List<Vente>> getAllVentes() {
        List<Vente> ventes = venteService.getAllVentes();
        return ResponseEntity.ok(ventes);
    }







}

