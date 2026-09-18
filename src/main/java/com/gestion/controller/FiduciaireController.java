package com.gestion.controller;

import com.acommon.persistant.dto.JwtAuthenticationResponse;
import com.gestion.persistent.dto.DashboardFiduciaireDTO;
import com.gestion.persistent.dto.EcheanceFiscaleDTO;
import com.gestion.persistent.dto.MereDTO;
import com.gestion.persistent.dto.SocieteDTO;
import com.gestion.service.FiduciaireService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/fiduciaire")
@CrossOrigin(origins = "*")
public class FiduciaireController {

    private final FiduciaireService fiduciaireService;

    public FiduciaireController(FiduciaireService fiduciaireService) {
        this.fiduciaireService = fiduciaireService;
    }

    // =========================================================================
    // SWITCH DE SOCIÉTÉ ACTIVE (SANS HEADER CUSTOM)
    // =========================================================================

    @PostMapping("/switch-societe/{id}")
    public ResponseEntity<JwtAuthenticationResponse> switchSociete(@PathVariable Long id) {
        return ResponseEntity.ok(fiduciaireService.switchSociete(id));
    }

    @GetMapping("/mes-societes")
    public ResponseEntity<List<SocieteDTO>> listerMesSocietes() {
        return ResponseEntity.ok(fiduciaireService.listerMesSocietes());
    }

    // =========================================================================
    // GESTION DES MÈRES (CABINETS / HOLDINGS)
    // =========================================================================

    @PostMapping("/meres")
    public ResponseEntity<MereDTO> creerMere(@RequestBody MereDTO dto) {
        return new ResponseEntity<>(fiduciaireService.creerMere(dto), HttpStatus.CREATED);
    }

    @GetMapping("/meres")
    public ResponseEntity<List<MereDTO>> listerMeres() {
        return ResponseEntity.ok(fiduciaireService.listerMeres());
    }

    @GetMapping("/meres/{id}")
    public ResponseEntity<MereDTO> getMere(@PathVariable Long id) {
        return ResponseEntity.ok(fiduciaireService.getMere(id));
    }

    // =========================================================================
    // CRUD MULTI-SOCIÉTÉS
    // =========================================================================

    @PostMapping("/societes")
    public ResponseEntity<SocieteDTO> creerSociete(@RequestBody SocieteDTO dto) {
        return new ResponseEntity<>(fiduciaireService.creerSociete(dto), HttpStatus.CREATED);
    }

    @GetMapping("/societes")
    public ResponseEntity<List<SocieteDTO>> listerSocietes() {
        return ResponseEntity.ok(fiduciaireService.listerSocietes());
    }

    @GetMapping("/societes/{id}")
    public ResponseEntity<SocieteDTO> getSociete(@PathVariable Long id) {
        return ResponseEntity.ok(fiduciaireService.getSociete(id));
    }

    @PutMapping("/societes/{id}")
    public ResponseEntity<SocieteDTO> modifierSociete(@PathVariable Long id, @RequestBody SocieteDTO dto) {
        return ResponseEntity.ok(fiduciaireService.modifierSociete(id, dto));
    }

    @DeleteMapping("/societes/{id}")
    public ResponseEntity<Void> supprimerSociete(@PathVariable Long id) {
        fiduciaireService.supprimerSociete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/societes/{id}/defaut")
    public ResponseEntity<SocieteDTO> setSocieteParDefaut(@PathVariable Long id) {
        return ResponseEntity.ok(fiduciaireService.setSocieteParDefaut(id));
    }

    @PostMapping("/societes/{id}/generer-echeances")
    public ResponseEntity<List<EcheanceFiscaleDTO>> genererEcheances(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "2026") int annee) {
        return ResponseEntity.ok(fiduciaireService.genererEcheancierFiscalAnnuel(id, annee));
    }

    // =========================================================================
    // CONSOLE & DASHBOARD FIDUCIAIRE
    // =========================================================================

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardFiduciaireDTO> getDashboardFiduciaire() {
        return ResponseEntity.ok(fiduciaireService.getDashboardFiduciaire());
    }

    @PatchMapping("/echeances/{id}/statut")
    public ResponseEntity<EcheanceFiscaleDTO> marquerEcheanceStatut(
            @PathVariable Long id,
            @RequestParam String statut,
            @RequestParam(required = false) BigDecimal montantPaye,
            @RequestParam(required = false) String referencePaiement) {
        return ResponseEntity.ok(fiduciaireService.marquerEcheanceStatut(id, statut, montantPaye, referencePaiement));
    }

    // =========================================================================
    // AFFECTATION COLLABORATEURS PAR DOSSIER
    // =========================================================================

    @PostMapping("/societes/{societeId}/collaborateurs/{userId}")
    public ResponseEntity<com.gestion.persistent.dto.CollaborateurSocieteDTO> assignerCollaborateur(
            @PathVariable Long societeId,
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "GESTIONNAIRE") String roleDossier) {
        return ResponseEntity.ok(fiduciaireService.assignerCollaborateur(societeId, userId, roleDossier));
    }

    @DeleteMapping("/societes/{societeId}/collaborateurs/{userId}")
    public ResponseEntity<Void> retirerCollaborateur(
            @PathVariable Long societeId,
            @PathVariable Long userId) {
        fiduciaireService.retirerCollaborateur(societeId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/societes/{societeId}/collaborateurs")
    public ResponseEntity<List<com.gestion.persistent.dto.CollaborateurSocieteDTO>> listerCollaborateursSociete(
            @PathVariable Long societeId) {
        return ResponseEntity.ok(fiduciaireService.listerCollaborateursSociete(societeId));
    }
}
