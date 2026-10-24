package com.gestion.controller;

import com.gestion.persistent.dto.ChargeAchatDTO;
import com.gestion.service.ChargeAchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ChargeAchatController {

    private final ChargeAchatService chargeAchatService;

    public ChargeAchatController(ChargeAchatService chargeAchatService) {
        this.chargeAchatService = chargeAchatService;
    }

    @GetMapping("/commandes/{commandeId}/charges")
    public ResponseEntity<List<ChargeAchatDTO>> listerCharges(@PathVariable Long commandeId) {
        return ResponseEntity.ok(chargeAchatService.listerChargesParCommande(commandeId));
    }

    @GetMapping("/charges-achat/{id}")
    public ResponseEntity<ChargeAchatDTO> getCharge(@PathVariable Long id) {
        return ResponseEntity.ok(chargeAchatService.getChargeById(id));
    }

    @PostMapping("/commandes/{commandeId}/charges")
    public ResponseEntity<ChargeAchatDTO> ajouterCharge(@PathVariable Long commandeId,
                                                         @RequestBody ChargeAchatDTO dto) {
        return ResponseEntity.ok(chargeAchatService.ajouterCharge(commandeId, dto));
    }

    @PutMapping("/charges-achat/{id}")
    public ResponseEntity<ChargeAchatDTO> modifierCharge(@PathVariable Long id,
                                                         @RequestBody ChargeAchatDTO dto) {
        return ResponseEntity.ok(chargeAchatService.modifierCharge(id, dto));
    }

    @DeleteMapping("/charges-achat/{id}")
    public ResponseEntity<Void> supprimerCharge(@PathVariable Long id) {
        chargeAchatService.supprimerCharge(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/charges-achat/{id}/lier-facture/{factureId}")
    public ResponseEntity<ChargeAchatDTO> lierFacture(@PathVariable Long id,
                                                      @PathVariable Long factureId) {
        return ResponseEntity.ok(chargeAchatService.lierFactureAchat(id, factureId));
    }
}
