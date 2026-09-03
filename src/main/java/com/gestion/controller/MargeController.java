package com.gestion.controller;

import com.gestion.persistent.dto.MargeDTO;
import com.gestion.persistent.dto.MargeDTO.LigneMargeDTO;
import com.gestion.service.MargeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
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
    @PreAuthorize("hasAnyAuthority('MARGES_VOIR', 'RAPPORT_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<MargeDTO> getMargeGlobale(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        MargeDTO dto = margeService.calculerMargeGlobale(dateDebut, dateFin);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/produits")
    @PreAuthorize("hasAnyAuthority('MARGES_VOIR', 'RAPPORT_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParProduit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();
        List<LigneMargeDTO> list = margeService.calculerMargeParProduit(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('MARGES_VOIR', 'RAPPORT_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParCategorie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();
        List<LigneMargeDTO> list = margeService.calculerMargeParCategorie(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX));
        return ResponseEntity.ok(list);
    }

    @GetMapping("/clients")
    @PreAuthorize("hasAnyAuthority('MARGES_VOIR', 'RAPPORT_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<LigneMargeDTO>> getMargesParClient(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        if (dateDebut == null) dateDebut = LocalDate.now().withDayOfMonth(1);
        if (dateFin == null) dateFin = LocalDate.now();
        List<LigneMargeDTO> list = margeService.calculerMargeParClient(dateDebut.atStartOfDay(), dateFin.atTime(LocalTime.MAX));
        return ResponseEntity.ok(list);
    }
}
