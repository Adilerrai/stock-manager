package com.ceramique.controller;

import com.ceramique.persistent.model.BonLivraisonClient;
import com.ceramique.service.BonLivraisonClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/livraisons-client")
public class BonLivraisonClientController {

    private final BonLivraisonClientService bonLivraisonClientService;

    public BonLivraisonClientController(BonLivraisonClientService bonLivraisonClientService) {
        this.bonLivraisonClientService = bonLivraisonClientService;
    }

    @PostMapping
    public ResponseEntity<BonLivraisonClient> creerBonLivraisonClient(@RequestBody BonLivraisonClient bl) {
        return ResponseEntity.ok(bonLivraisonClientService.creerBonLivraisonClient(bl));
    }

    @PostMapping("/{id}/valider")
    public ResponseEntity<BonLivraisonClient> validerEtExpedierBL(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.validerEtExpedierBL(id));
    }

    @GetMapping
    public ResponseEntity<List<BonLivraisonClient>> getBonsLivraison() {
        return ResponseEntity.ok(bonLivraisonClientService.getBonsLivraison());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonLivraisonClient> getBonLivraisonById(@PathVariable Long id) {
        return ResponseEntity.ok(bonLivraisonClientService.getBonLivraisonById(id));
    }
}
