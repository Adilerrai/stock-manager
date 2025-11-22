package com.ceramique.controller;

import com.ceramique.persistent.enums.ModePaiement;
import com.ceramique.persistent.model.Paiement;
import com.ceramique.service.PaiementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/paiements")
@CrossOrigin(origins = "*")
public class PaiementController {

    private final PaiementService paiementService;

    public PaiementController(PaiementService paiementService) {
        this.paiementService = paiementService;
    }

    @PostMapping("/vente/{venteId}")
    public ResponseEntity<Paiement> enregistrerPaiementVente(@PathVariable Long venteId,
                                                              @RequestBody Paiement paiement,
                                                              @RequestParam Long userId) {
        Paiement nouveauPaiement = paiementService.enregistrerPaiementVente(venteId, paiement, userId);
        return new ResponseEntity<>(nouveauPaiement, HttpStatus.CREATED);
    }

    @PostMapping("/facture/{factureId}")
    public ResponseEntity<Paiement> enregistrerPaiementFacture(@PathVariable Long factureId,
                                                                @RequestBody Paiement paiement,
                                                                @RequestParam Long userId) {
        Paiement nouveauPaiement = paiementService.enregistrerPaiementFacture(factureId, paiement, userId);
        return new ResponseEntity<>(nouveauPaiement, HttpStatus.CREATED);
    }

    @PostMapping("/{paiementId}/annuler")
    public ResponseEntity<Paiement> annulerPaiement(@PathVariable Long paiementId,
                                                     @RequestParam String motif,
                                                     @RequestParam Long userId) {
        Paiement paiement = paiementService.annulerPaiement(paiementId, motif, userId);
        return ResponseEntity.ok(paiement);
    }

    @GetMapping("/vente/{venteId}")
    public ResponseEntity<List<Paiement>> getPaiementsByVente(@PathVariable Long venteId) {
        List<Paiement> paiements = paiementService.getPaiementsByVente(venteId);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/facture/{factureId}")
    public ResponseEntity<List<Paiement>> getPaiementsByFacture(@PathVariable Long factureId) {
        List<Paiement> paiements = paiementService.getPaiementsByFacture(factureId);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<Paiement>> getPaiementsByClient(@PathVariable Long clientId) {
        List<Paiement> paiements = paiementService.getPaiementsByClient(clientId);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/periode")
    public ResponseEntity<List<Paiement>> getPaiementsByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        List<Paiement> paiements = paiementService.getPaiementsByPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(paiements);
    }

    @GetMapping("/total-periode")
    public ResponseEntity<Map<String, Object>> getTotalPaiementsByPeriode(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        BigDecimal total = paiementService.getTotalPaiementsByPeriode(dateDebut, dateFin);
        return ResponseEntity.ok(Map.of("total", total));
    }

    @GetMapping("/total-mode-paiement")
    public ResponseEntity<Map<String, Object>> getTotalByModePaiement(
            @RequestParam ModePaiement modePaiement,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin) {
        BigDecimal total = paiementService.getTotalPaiementsByModePaiement(modePaiement, dateDebut, dateFin);
        return ResponseEntity.ok(Map.of("total", total, "modePaiement", modePaiement.name()));
    }
}

