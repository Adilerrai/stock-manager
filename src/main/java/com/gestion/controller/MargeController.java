package com.gestion.controller;

import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.MargeDTO.LigneMargeDTO;
import com.gestion.service.MargeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/marges")
@CrossOrigin(origins = "*")
public class MargeController {

    private final MargeService margeService;

    public MargeController(MargeService margeService) {
        this.margeService = margeService;
    }

    @GetMapping
    public ResponseEntity<MargeDTO> getMargeGlobale(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        MargeDTO dto = margeService.calculerMargeGlobale(dateDebut, dateFin);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/produits")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParProduit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<LigneMargeDTO> list = margeService.calculerMargeParProduit(dateDebut, dateFin);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/categories")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParCategorie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<LigneMargeDTO> list = margeService.calculerMargeParCategorie(dateDebut, dateFin);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/clients")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParClient(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        List<LigneMargeDTO> list = margeService.calculerMargeParClient(dateDebut, dateFin);
        return ResponseEntity.ok(list);
    }
}
