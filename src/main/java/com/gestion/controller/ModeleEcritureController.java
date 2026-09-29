package com.gestion.controller;

import com.gestion.persistent.dto.AppliquerModeleRequest;
import com.gestion.persistent.dto.EcritureComptableDTO;
import com.gestion.persistent.dto.ModeleEcritureDTO;
import com.gestion.service.ModeleEcritureService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comptabilite/modeles")
@CrossOrigin(origins = "*")
public class ModeleEcritureController {

    private final ModeleEcritureService modeleEcritureService;

    public ModeleEcritureController(ModeleEcritureService modeleEcritureService) {
        this.modeleEcritureService = modeleEcritureService;
    }

    /**
     * Liste de tous les modèles d'écritures récurrentes disponibles
     */
    @GetMapping
    public ResponseEntity<List<ModeleEcritureDTO>> getModelesDisponibles() {
        return ResponseEntity.ok(modeleEcritureService.getModelesDisponibles());
    }

    /**
     * Consultation du détail d'un modèle spécifique (lignes, comptes, type de calculs)
     */
    @GetMapping("/{code}")
    public ResponseEntity<ModeleEcritureDTO> getModeleByCode(@PathVariable String code) {
        return ResponseEntity.ok(modeleEcritureService.getModeleByCode(code));
    }

    /**
     * Application d'un modèle récurrent avec saisie guidée (montant TTC ou HT)
     * et génération automatique de l'écriture équilibrée dans le journal cible.
     */
    @PostMapping("/appliquer")
    public ResponseEntity<EcritureComptableDTO> appliquerModele(@RequestBody AppliquerModeleRequest request) {
        return new ResponseEntity<>(modeleEcritureService.appliquerModele(request), HttpStatus.CREATED);
    }
}
