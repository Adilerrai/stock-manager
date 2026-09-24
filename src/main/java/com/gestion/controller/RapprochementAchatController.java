package com.gestion.controller;

import com.gestion.persistent.dto.RapprochementAchatDTO;
import com.gestion.service.RapprochementAchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/achats/rapprochements")
@CrossOrigin(origins = "*")
public class RapprochementAchatController {

    private final RapprochementAchatService rapprochementService;

    public RapprochementAchatController(RapprochementAchatService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }

    /**
     * Effectue le contrôle 3-Way d'une facture d'achat fournisseur
     * (Commande <-> Réception Dépôt <-> Facture Achat)
     */
    @GetMapping("/{factureAchatId}")
    public ResponseEntity<RapprochementAchatDTO> rapprocherFacture(@PathVariable Long factureAchatId) {
        return ResponseEntity.ok(rapprochementService.rapprocherFactureAchat(factureAchatId));
    }

    /**
     * Liste des factures d'achats avec litiges et paiements bloqués
     */
    @GetMapping("/litiges")
    public ResponseEntity<List<RapprochementAchatDTO>> getLitiges() {
        return ResponseEntity.ok(rapprochementService.getRapprochementsLitigieux());
    }
}
