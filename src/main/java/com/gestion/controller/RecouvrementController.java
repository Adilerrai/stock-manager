package com.gestion.controller;

import com.gestion.persistent.dto.PromessePaiementDTO;
import com.gestion.persistent.dto.RelanceClientDTO;
import com.gestion.persistent.enums.StatutPromesse;
import com.gestion.service.RecouvrementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recouvrement")
@CrossOrigin(origins = "*")
public class RecouvrementController {

    private final RecouvrementService recouvrementService;

    public RecouvrementController(RecouvrementService recouvrementService) {
        this.recouvrementService = recouvrementService;
    }

    @PostMapping("/relances")
    @PreAuthorize("hasAnyAuthority('CLIENT_VOIR', 'TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<RelanceClientDTO> enregistrerRelance(
            @RequestBody RelanceClientDTO dto,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(recouvrementService.enregistrerRelance(dto, userId), HttpStatus.CREATED);
    }

    @GetMapping("/relances/client/{clientId}")
    @PreAuthorize("hasAnyAuthority('CLIENT_VOIR', 'TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<RelanceClientDTO>> getRelancesClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(recouvrementService.getRelancesParClient(clientId));
    }

    @PostMapping("/promesses")
    @PreAuthorize("hasAnyAuthority('CLIENT_VOIR', 'TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMMERCIAL', 'ROLE_COMPTABLE')")
    public ResponseEntity<PromessePaiementDTO> enregistrerPromesse(
            @RequestBody PromessePaiementDTO dto,
            @RequestParam(required = false) Long userId) {
        return new ResponseEntity<>(recouvrementService.enregistrerPromesse(dto, userId), HttpStatus.CREATED);
    }

    @PatchMapping("/promesses/{id}/statut")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<PromessePaiementDTO> changerStatutPromesse(
            @PathVariable Long id,
            @RequestParam StatutPromesse statut) {
        return ResponseEntity.ok(recouvrementService.marquerStatutPromesse(id, statut));
    }

    @GetMapping("/promesses/en-cours")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<PromessePaiementDTO>> getPromessesEnCours() {
        return ResponseEntity.ok(recouvrementService.getPromessesEnCours());
    }

    @GetMapping("/promesses/rompues")
    @PreAuthorize("hasAnyAuthority('TRESORERIE_GESTION', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<PromessePaiementDTO>> getPromessesRompues() {
        return ResponseEntity.ok(recouvrementService.getPromessesRompues());
    }

    @GetMapping("/promesses/client/{clientId}")
    @PreAuthorize("hasAnyAuthority('CLIENT_VOIR', 'TRESORERIE_GESTION', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<PromessePaiementDTO>> getPromessesClient(@PathVariable Long clientId) {
        return ResponseEntity.ok(recouvrementService.getPromessesParClient(clientId));
    }
}
