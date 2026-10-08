package com.gestion.controller;

import com.gestion.persistent.dto.AnalyseProduitStockDTO;
import com.gestion.persistent.dto.RapportAnalyseStockDTO;
import com.gestion.service.RapportStockService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/rapports/stock", "/api/rapports/stock"})
@CrossOrigin(origins = "*")
public class RapportStockController {

    private final RapportStockService rapportStockService;

    public RapportStockController(RapportStockService rapportStockService) {
        this.rapportStockService = rapportStockService;
    }

    /**
     * Cockpit décisionnel d'état & rotation des stocks :
     * - Valeur totale PMP
     * - 324 références & alertes seuils bas
     * - Taux de rotation moyen & délai d'écoulement en jours
     * - Valorisation exacte par catégorie (résolution du 21.8%)
     * - Détection comportementale : Sous-stock, Sur-stock (capital dormant), Normal, Rupture
     * - Matrice croisée ABC x Rotation
     */
    @GetMapping("/analyse")
    public ResponseEntity<RapportAnalyseStockDTO> getRapportAnalyseStock(
            @RequestParam(required = false) Long categorieId,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false, defaultValue = "90") Integer joursHistorique) {
        
        RapportAnalyseStockDTO rapport = rapportStockService.genererRapportAnalyse(categorieId, depotId, joursHistorique);
        return ResponseEntity.ok(rapport);
    }

    /**
     * Liste des articles nécessitant un réapprovisionnement urgent (couverture faible ou rupture).
     */
    @GetMapping("/recommandations-reappro")
    public ResponseEntity<List<AnalyseProduitStockDTO>> getRecommandationsReapprovisionnement() {
        return ResponseEntity.ok(rapportStockService.getRecommandationsReapprovisionnement());
    }
}
