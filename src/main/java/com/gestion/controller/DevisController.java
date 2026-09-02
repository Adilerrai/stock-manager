package com.gestion.controller;

import com.gestion.persistent.enums.StatutDevis;
import com.gestion.persistent.model.CommandeClient;
import com.gestion.persistent.model.Devis;
import com.gestion.persistent.model.Facture;
import com.gestion.service.DevisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devis")
@CrossOrigin(origins = "*")
public class DevisController {

    private final DevisService devisService;

    public DevisController(DevisService devisService) {
        this.devisService = devisService;
    }

    @PostMapping
    public ResponseEntity<Devis> creerDevis(@RequestBody Devis devis,
                                           @RequestParam(required = false) Long userId) {
        Devis nouveauDevis = devisService.creerDevis(devis, userId);
        return new ResponseEntity<>(nouveauDevis, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Devis> modifierDevis(@PathVariable Long id,
                                               @RequestBody Devis devis) {
        Devis modifie = devisService.modifierDevis(id, devis);
        return ResponseEntity.ok(modifie);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Devis> getDevisById(@PathVariable Long id) {
        return ResponseEntity.ok(devisService.getDevisById(id));
    }

    @GetMapping
    public ResponseEntity<List<Devis>> getAllDevis() {
        return ResponseEntity.ok(devisService.getAllDevis());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Devis>> getDevisByClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(devisService.getDevisByClient(clientId));
    }

    @GetMapping("/statut/{statut}")
    public ResponseEntity<List<Devis>> getDevisByStatut(@PathVariable StatutDevis statut) {
        return ResponseEntity.ok(devisService.getDevisByStatut(statut));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<Devis> changerStatut(@PathVariable Long id,
                                               @RequestParam StatutDevis statut) {
        return ResponseEntity.ok(devisService.changerStatut(id, statut));
    }

    @PostMapping("/{id}/convertir-commande")
    public ResponseEntity<CommandeClient> convertirEnCommande(@PathVariable Long id,
                                                              @RequestParam(required = false) Long userId) {
        CommandeClient commande = devisService.transformerEnCommandeClient(id, userId);
        return new ResponseEntity<>(commande, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/convertir-facture")
    public ResponseEntity<Facture> convertirEnFacture(@PathVariable Long id,
                                                      @RequestParam(required = false) Long userId) {
        Facture facture = devisService.transformerEnFacture(id, userId);
        return new ResponseEntity<>(facture, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerDevis(@PathVariable Long id) {
        devisService.supprimerDevis(id);
        return ResponseEntity.noContent().build();
    }
}
