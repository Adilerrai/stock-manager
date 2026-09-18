package com.gestion.controller;

import com.gestion.persistent.dto.AxeAnalytiqueDTO;
import com.gestion.persistent.dto.CentreAnalytiqueDTO;
import com.gestion.persistent.dto.CpcAnalytiqueDTO;
import com.gestion.persistent.dto.VentilationAnalytiqueDTO;
import com.gestion.service.AnalytiqueService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytique")
@CrossOrigin(origins = "*")
public class AnalytiqueController {

    private final AnalytiqueService analytiqueService;

    public AnalytiqueController(AnalytiqueService analytiqueService) {
        this.analytiqueService = analytiqueService;
    }

    // =========================================================================
    // AXES ANALYTIQUES
    // =========================================================================

    @PostMapping("/axes")
    public ResponseEntity<AxeAnalytiqueDTO> creerAxe(@RequestBody AxeAnalytiqueDTO dto) {
        return new ResponseEntity<>(analytiqueService.creerAxe(dto), HttpStatus.CREATED);
    }

    @GetMapping("/axes")
    public ResponseEntity<List<AxeAnalytiqueDTO>> listerAxes() {
        return ResponseEntity.ok(analytiqueService.listerAxes());
    }

    @PutMapping("/axes/{id}")
    public ResponseEntity<AxeAnalytiqueDTO> modifierAxe(@PathVariable Long id, @RequestBody AxeAnalytiqueDTO dto) {
        return ResponseEntity.ok(analytiqueService.modifierAxe(id, dto));
    }

    @DeleteMapping("/axes/{id}")
    public ResponseEntity<Void> supprimerAxe(@PathVariable Long id) {
        analytiqueService.supprimerAxe(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // CENTRES / SECTIONS ANALYTIQUES
    // =========================================================================

    @PostMapping("/centres")
    public ResponseEntity<CentreAnalytiqueDTO> creerCentre(@RequestBody CentreAnalytiqueDTO dto) {
        return new ResponseEntity<>(analytiqueService.creerCentre(dto), HttpStatus.CREATED);
    }

    @GetMapping("/centres")
    public ResponseEntity<List<CentreAnalytiqueDTO>> listerCentres(@RequestParam(required = false) Long axeId) {
        return ResponseEntity.ok(analytiqueService.listerCentres(axeId));
    }

    @PutMapping("/centres/{id}")
    public ResponseEntity<CentreAnalytiqueDTO> modifierCentre(@PathVariable Long id, @RequestBody CentreAnalytiqueDTO dto) {
        return ResponseEntity.ok(analytiqueService.modifierCentre(id, dto));
    }

    @DeleteMapping("/centres/{id}")
    public ResponseEntity<Void> supprimerCentre(@PathVariable Long id) {
        analytiqueService.supprimerCentre(id);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // VENTILATION ANALYTIQUE
    // =========================================================================

    @PostMapping("/lignes/{ligneId}/ventiler")
    public ResponseEntity<List<VentilationAnalytiqueDTO>> ventilerLigne(
            @PathVariable Long ligneId,
            @RequestBody List<VentilationAnalytiqueDTO> ventilations) {
        return ResponseEntity.ok(analytiqueService.ventilerLigneEcriture(ligneId, ventilations));
    }

    @GetMapping("/lignes/{ligneId}/ventilations")
    public ResponseEntity<List<VentilationAnalytiqueDTO>> getVentilationsLigne(@PathVariable Long ligneId) {
        return ResponseEntity.ok(analytiqueService.getVentilationsLigne(ligneId));
    }

    // =========================================================================
    // ÉTATS ANALYTIQUES & RENTABILITÉ
    // =========================================================================

    @GetMapping("/cpc/{centreId}")
    public ResponseEntity<CpcAnalytiqueDTO> getCpcAnalytique(
            @PathVariable Long centreId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(analytiqueService.getCpcAnalytique(centreId, debut, fin));
    }

    @GetMapping("/axes/{axeId}/rentabilite")
    public ResponseEntity<List<CpcAnalytiqueDTO>> getRentabiliteAxe(
            @PathVariable Long axeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(analytiqueService.getRentabiliteAxe(axeId, debut, fin));
    }
}
