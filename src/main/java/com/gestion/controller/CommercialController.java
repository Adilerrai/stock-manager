package com.gestion.controller;

import com.gestion.persistent.dto.PerformanceCommercialDTO;
import com.gestion.persistent.model.ObjectifCommercial;
import com.gestion.service.CommercialService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/commerciaux")
@CrossOrigin(origins = "*")
public class CommercialController {

    private final CommercialService commercialService;

    public CommercialController(CommercialService commercialService) {
        this.commercialService = commercialService;
    }

    @GetMapping("/performances")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL')")
    public ResponseEntity<List<PerformanceCommercialDTO>> getPerformances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(commercialService.getPerformancesCommerciaux(dateDebut, dateFin));
    }

    @GetMapping("/{id}/performance")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMMERCIAL')")
    public ResponseEntity<PerformanceCommercialDTO> getPerformance(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(commercialService.getPerformanceByCommercial(id, dateDebut, dateFin));
    }

    @PostMapping("/objectifs")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL')")
    public ResponseEntity<ObjectifCommercial> definirObjectif(
            @RequestParam Long commercialId,
            @RequestParam Integer annee,
            @RequestParam Integer mois,
            @RequestParam BigDecimal objectifCA,
            @RequestParam(required = false) BigDecimal objectifMarge,
            @RequestParam(required = false) String notes) {
        ObjectifCommercial obj = commercialService.definirObjectif(commercialId, annee, mois, objectifCA, objectifMarge, notes);
        return new ResponseEntity<>(obj, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/objectifs")
    @PreAuthorize("hasAnyAuthority('COMMERCIAL_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_RESPONSABLE_COMMERCIAL', 'ROLE_COMMERCIAL')")
    public ResponseEntity<List<ObjectifCommercial>> getObjectifs(@PathVariable Long id) {
        return ResponseEntity.ok(commercialService.getObjectifsByCommercial(id));
    }
}
