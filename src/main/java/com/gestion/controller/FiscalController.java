package com.gestion.controller;

import com.gestion.persistent.dto.CalculIsDTO;
import com.gestion.persistent.dto.CalendrierFiscalDTO;
import com.gestion.persistent.dto.EcheancierIsDTO;
import com.gestion.persistent.model.RegleFiscaleIS;
import com.gestion.service.FiscalEngineService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/fiscalite")
@CrossOrigin(origins = "*")
public class FiscalController {

    private final FiscalEngineService fiscalEngineService;

    public FiscalController(FiscalEngineService fiscalEngineService) {
        this.fiscalEngineService = fiscalEngineService;
    }

    // =========================================================================
    // CALCUL IS & COTISATION MINIMALE (DONNÉES COMPTABLES RÉELLES)
    // =========================================================================

    @GetMapping("/is/calcul")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<CalculIsDTO> calculerIs(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false, defaultValue = "0") BigDecimal reintegrations,
            @RequestParam(required = false, defaultValue = "0") BigDecimal deductions) {
        int anneeFiscale = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(fiscalEngineService.calculerIs(anneeFiscale, reintegrations, deductions));
    }

    // =========================================================================
    // ÉCHÉANCIER DES 4 ACOMPTES TRIMESTRIELS & RELIQUAT
    // =========================================================================

    @GetMapping("/is/echeancier-acomptes")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<EcheancierIsDTO> getEcheancierAcomptes(
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) BigDecimal impotReference) {
        int anneeFiscale = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(fiscalEngineService.genererEcheancierAcomptes(anneeFiscale, impotReference));
    }

    // =========================================================================
    // CALENDRIER FISCAL DGI COMPLET (TVA, IS, LIASSE)
    // =========================================================================

    @GetMapping("/calendrier")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<CalendrierFiscalDTO> getCalendrierFiscal(
            @RequestParam(required = false) Integer annee) {
        int anneeFiscale = (annee != null) ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(fiscalEngineService.getCalendrierFiscal(anneeFiscale));
    }

    // =========================================================================
    // PARAMÉTRAGE DES RÈGLES FISCALES VERSIONNÉES (LOI DE FINANCES)
    // =========================================================================

    @GetMapping("/regles/{annee}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<RegleFiscaleIS> getRegleFiscale(@PathVariable Integer annee) {
        return ResponseEntity.ok(fiscalEngineService.getOrCreateRegleFiscale(annee));
    }

    @PutMapping("/regles/{annee}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<RegleFiscaleIS> modifierRegleFiscale(
            @PathVariable Integer annee,
            @RequestBody RegleFiscaleIS dto) {
        return ResponseEntity.ok(fiscalEngineService.modifierRegleFiscale(annee, dto));
    }
}
