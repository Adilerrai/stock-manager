package com.gestion.controller;

import com.gestion.persistent.dto.RapprochementAchatDTO;
import com.gestion.service.RapprochementAchatService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyAuthority('ACHAT_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<RapprochementAchatDTO> rapprocherFacture(@PathVariable Long factureAchatId) {
        return ResponseEntity.ok(rapprochementService.rapprocherFactureAchat(factureAchatId));
    }

    /**
     * Liste des factures d'achats avec litiges et paiements bloqués
     */
    @GetMapping("/litiges")
    @PreAuthorize("hasAnyAuthority('ACHAT_VOIR', 'DASHBOARD_VOIR', 'ROLE_ADMIN', 'ROLE_GESTIONNAIRE', 'ROLE_POINT_DE_VENTE_MANAGER', 'ROLE_COMPTABLE')")
    public ResponseEntity<List<RapprochementAchatDTO>> getLitiges() {
        return ResponseEntity.ok(rapprochementService.getRapprochementsLitigieux());
    }
}
