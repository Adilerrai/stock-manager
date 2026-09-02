package com.gestion.controller;

import com.gestion.persistent.model.VarianteProduit;
import com.gestion.service.VarianteProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/variantes")
@CrossOrigin(origins = "*")
public class VarianteProduitController {

    private final VarianteProduitService varianteService;

    public VarianteProduitController(VarianteProduitService varianteService) {
        this.varianteService = varianteService;
    }

    @PostMapping("/produit/{produitId}")
    public ResponseEntity<VarianteProduit> ajouterVariante(@PathVariable Long produitId,
                                                          @RequestBody VarianteProduit variante) {
        VarianteProduit nouvelle = varianteService.ajouterVariante(produitId, variante);
        return new ResponseEntity<>(nouvelle, HttpStatus.CREATED);
    }

    @GetMapping("/produit/{produitId}")
    public ResponseEntity<List<VarianteProduit>> getVariantesByProduit(@PathVariable Long produitId) {
        return ResponseEntity.ok(varianteService.getVariantesByProduit(produitId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VarianteProduit> getVarianteById(@PathVariable Long id) {
        return ResponseEntity.ok(varianteService.getVarianteById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VarianteProduit> modifierVariante(@PathVariable Long id,
                                                            @RequestBody VarianteProduit variante) {
        return ResponseEntity.ok(varianteService.modifierVariante(id, variante));
    }

    @PatchMapping("/{id}/stock")
    public ResponseEntity<VarianteProduit> ajusterStock(@PathVariable Long id,
                                                        @RequestParam BigDecimal delta) {
        return ResponseEntity.ok(varianteService.ajusterStock(id, delta));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimerVariante(@PathVariable Long id) {
        varianteService.supprimerVariante(id);
        return ResponseEntity.noContent().build();
    }
}
