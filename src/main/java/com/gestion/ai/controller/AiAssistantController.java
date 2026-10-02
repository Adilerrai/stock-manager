package com.gestion.ai.controller;

import com.gestion.ai.dto.AiChatRequestDTO;
import com.gestion.ai.dto.AiChatResponseDTO;
import com.gestion.ai.security.AiTenantContextEnricher;
import com.gestion.ai.security.AiTenantScope;
import com.gestion.ai.service.AiAssistantService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;
    private final AiTenantContextEnricher tenantEnricher;

    public AiAssistantController(AiAssistantService aiAssistantService,
                                 AiTenantContextEnricher tenantEnricher) {
        this.aiAssistantService = aiAssistantService;
        this.tenantEnricher = tenantEnricher;
    }

    /**
     * Endpoint principal de dialogue avec l'assistant IA ERP
     */
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDTO> chat(@RequestBody AiChatRequestDTO request) {
        AiChatResponseDTO response = aiAssistantService.chat(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retourne le périmètre de sécurité multi-tenant actif pour l'IA
     */
    @GetMapping("/scope")
    public ResponseEntity<Map<String, Object>> getScope() {
        AiTenantScope scope = tenantEnricher.resolveCurrentScope();
        return ResponseEntity.ok(Map.of(
                "tenantId", scope.getTenantId(),
                "mereId", scope.getMereId() != null ? scope.getMereId() : "N/A",
                "holding", scope.isHolding(),
                "accessibleTenantIds", scope.getAccessibleTenantIds(),
                "nomEntreprise", scope.getNomEntreprise(),
                "role", scope.getRole()
        ));
    }

    /**
     * Liste des outils connectés au LLM
     */
    @GetMapping("/tools")
    public ResponseEntity<List<Map<String, String>>> getAvailableTools() {
        List<Map<String, String>> tools = List.of(
                Map.of("id", "consulterStock", "name", "Consultation de Stock", "category", "Stock"),
                Map.of("id", "alertesRuptureStock", "name", "Alertes Rupture de Stock", "category", "Stock"),
                Map.of("id", "rechercherClient", "name", "Recherche Client & Encours", "category", "Commercial"),
                Map.of("id", "clientsDepassementCredit", "name", "Dépassement Plafond Crédit", "category", "Risque Client"),
                Map.of("id", "totalCreancesImpayees", "name", "Bilan Créances & Retards", "category", "Comptabilité"),
                Map.of("id", "facturesImpayeesClient", "name", "Factures Impayées par Client", "category", "Recouvrement"),
                Map.of("id", "facturesEchues", "name", "Factures Échues en Retard", "category", "Recouvrement"),
                Map.of("id", "suiviCommandesEnCours", "name", "Suivi Commandes Clients", "category", "Ventes")
        );
        return ResponseEntity.ok(tools);
    }
}
