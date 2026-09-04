package com.gestion.controller;

import com.gestion.persistent.dto.*;
import com.gestion.service.ComptabiliteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/comptabilite")
@CrossOrigin(origins = "*")
public class ComptabiliteController {

    private final ComptabiliteService comptabiliteService;

    public ComptabiliteController(ComptabiliteService comptabiliteService) {
        this.comptabiliteService = comptabiliteService;
    }

    // =========================================================================
    // PLAN COMPTABLE
    // =========================================================================

    @GetMapping("/plan-comptable")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<CompteComptableDTO>> getPlanComptable() {
        return ResponseEntity.ok(comptabiliteService.getPlanComptable());
    }

    @PostMapping("/plan-comptable")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<CompteComptableDTO> creerCompte(@RequestBody CompteComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerCompte(dto), HttpStatus.CREATED);
    }

    // =========================================================================
    // JOURNAUX
    // =========================================================================

    @GetMapping("/journaux")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<JournalComptableDTO>> getJournaux() {
        return ResponseEntity.ok(comptabiliteService.getJournaux());
    }

    @PostMapping("/journaux")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<JournalComptableDTO> creerJournal(@RequestBody JournalComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerJournal(dto), HttpStatus.CREATED);
    }

    // =========================================================================
    // ÉCRITURES COMPTABLES
    // =========================================================================

    @GetMapping("/ecritures")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<EcritureComptableDTO>> getEcritures(
            @RequestParam(required = false) Long journalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getEcritures(journalId, dateDebut, dateFin));
    }

    @GetMapping("/ecritures/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<EcritureComptableDTO> getEcritureById(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.getEcritureById(id));
    }

    @PostMapping("/ecritures")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<EcritureComptableDTO> creerEcriture(@RequestBody EcritureComptableDTO dto) {
        return new ResponseEntity<>(comptabiliteService.creerEcriture(dto), HttpStatus.CREATED);
    }

    @PatchMapping("/ecritures/{id}/valider")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_COMPTABLE')")
    public ResponseEntity<EcritureComptableDTO> validerEcriture(@PathVariable Long id) {
        return ResponseEntity.ok(comptabiliteService.validerEcriture(id));
    }

    // =========================================================================
    // GRAND LIVRE & BALANCE & TVA
    // =========================================================================

    @GetMapping("/grand-livre")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<GrandLivreDTO>> getGrandLivre(
            @RequestParam(required = false) String numeroCompte,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getGrandLivre(numeroCompte, dateDebut, dateFin));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<List<BalanceCompteDTO>> getBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getBalance(dateDebut, dateFin));
    }

    @GetMapping("/tva")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_COMPTABLE', 'ROLE_POINT_DE_VENTE_MANAGER')")
    public ResponseEntity<DeclarationTvaDTO> getDeclarationTva(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(comptabiliteService.getDeclarationTva(dateDebut, dateFin));
    }
}
