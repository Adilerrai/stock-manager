package com.gestion.controller;

import com.gestion.persistent.dto.BonPreparationDTO;
import com.gestion.persistent.enums.StatutPreparation;
import com.gestion.service.BonPreparationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bons-preparation")
@CrossOrigin(origins = "*")
public class BonPreparationController {

    private final BonPreparationService bonPreparationService;

    public BonPreparationController(BonPreparationService bonPreparationService) {
        this.bonPreparationService = bonPreparationService;
    }

    @PostMapping("/depuis-commande/{commandeClientId}")
    @PreAuthorize("hasAnyAuthority('LIVRAISON_VALIDER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<BonPreparationDTO> genererDepuisCommande(
            @PathVariable Long commandeClientId,
            @RequestParam(required = false) Long magasinierId) {
        return new ResponseEntity<>(bonPreparationService.genererDepuisCommande(commandeClientId, magasinierId), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasAnyAuthority('LIVRAISON_VALIDER', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<BonPreparationDTO> validerPreparation(
            @PathVariable Long id,
            @RequestBody(required = false) Map<Long, BigDecimal> quantitesPreparees,
            @RequestParam(required = false) Long magasinierId) {
        return ResponseEntity.ok(bonPreparationService.validerPreparation(id, quantitesPreparees, magasinierId));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('LIVRAISON_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<List<BonPreparationDTO>> getTousLesBons() {
        return ResponseEntity.ok(bonPreparationService.getTousLesBons());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('LIVRAISON_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<BonPreparationDTO> getBonById(@PathVariable Long id) {
        return ResponseEntity.ok(bonPreparationService.getBonById(id));
    }

    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAnyAuthority('LIVRAISON_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_MAGASINIER')")
    public ResponseEntity<List<BonPreparationDTO>> getBonsParStatut(@PathVariable StatutPreparation statut) {
        return ResponseEntity.ok(bonPreparationService.getBonsParStatut(statut));
    }
}
