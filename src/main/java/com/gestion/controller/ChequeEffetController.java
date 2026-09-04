package com.gestion.controller;

import com.gestion.persistent.dto.ChequeEffetDTO;
import com.gestion.persistent.dto.PortefeuilleStatsDTO;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.service.ChequeEffetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import com.gestion.persistent.dto.ChequeEffetSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping({"/api/finance/cheques", "/api/cheques-effets"})
@CrossOrigin(origins = "*")
@Tag(name = "Finance - Chèques & Traites", description = "Gestion du portefeuille des effets de commerce et chèques (remise en banque, encaissement, rejets)")
public class ChequeEffetController {

    private final ChequeEffetService chequeEffetService;

    public ChequeEffetController(ChequeEffetService chequeEffetService) {
        this.chequeEffetService = chequeEffetService;
    }

    @PostMapping("/search")
    @Operation(summary = "Recherche dynamique multicritères de chèques et effets avec pagination")
    public ResponseEntity<Page<ChequeEffetDTO>> searchChequesEffets(
            @RequestBody ChequeEffetSearchCriteria criteria, Pageable pageable) {
        return ResponseEntity.ok(chequeEffetService.searchChequesEffets(criteria, pageable));
    }

    @GetMapping
    @Operation(summary = "Lister les chèques et effets (filtrable par statut et sens encaissement/décaissement)")
    public ResponseEntity<List<ChequeEffetDTO>> lister(
            @RequestParam(required = false) StatutEffet statut,
            @RequestParam(required = false) SensEffet sens) {
        return ResponseEntity.ok(chequeEffetService.listerCheques(statut, sens));
    }

    @PostMapping
    @Operation(summary = "Enregistrer un nouveau chèque ou effet dans le portefeuille")
    public ResponseEntity<ChequeEffetDTO> creer(@RequestBody ChequeEffetDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chequeEffetService.creerCheque(dto));
    }

    @PatchMapping("/{id}/remettre")
    @Operation(summary = "Remettre un chèque à l'encaissement (bordereau de remise)")
    public ResponseEntity<ChequeEffetDTO> remettreEnBanque(
            @PathVariable Long id,
            @RequestParam(required = false) String bordereauNumero,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateRemise) {
        return ResponseEntity.ok(chequeEffetService.remettreEnBanque(id, bordereauNumero, dateRemise));
    }

    @PatchMapping("/{id}/encaisser")
    @Operation(summary = "Confirmer l'encaissement d'un chèque sur le compte bancaire")
    public ResponseEntity<ChequeEffetDTO> confirmerEncaissement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEncaissement) {
        return ResponseEntity.ok(chequeEffetService.confirmerEncaissement(id, dateEncaissement));
    }

    @PatchMapping("/{id}/rejeter")
    @Operation(summary = "Déclarer un chèque rejeté / impayé par la banque")
    public ResponseEntity<ChequeEffetDTO> rejeterCheque(
            @PathVariable Long id,
            @RequestParam(required = false) String motif) {
        return ResponseEntity.ok(chequeEffetService.rejeterCheque(id, motif));
    }

    @GetMapping("/echeances-proches")
    @Operation(summary = "Lister les chèques dont la date d'échéance arrive bientôt (par défaut sous 7 jours)")
    public ResponseEntity<List<ChequeEffetDTO>> getEcheancesProches(@RequestParam(defaultValue = "7") int jours) {
        return ResponseEntity.ok(chequeEffetService.getChequesAEcheanceProche(jours));
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtenir les statistiques globales du portefeuille de chèques (en cours, remis, encaissés, impayés)")
    public ResponseEntity<PortefeuilleStatsDTO> getStats() {
        return ResponseEntity.ok(chequeEffetService.getStatsPortefeuille());
    }
}
