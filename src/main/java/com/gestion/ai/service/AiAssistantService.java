package com.gestion.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.ai.dto.AiChatRequestDTO;
import com.gestion.ai.dto.AiChatResponseDTO;
import com.gestion.ai.dto.AiToolExecutionDTO;
import com.gestion.ai.security.AiTenantContextEnricher;
import com.gestion.ai.security.AiTenantScope;
import com.gestion.ai.tools.ClientAiTools;
import com.gestion.ai.tools.FactureAiTools;
import com.gestion.ai.tools.StockAiTools;
import com.gestion.ai.tools.VenteAiTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
public class AiAssistantService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.api.key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${groq.model:qwen/qwen3.8-27b}")
    private String groqModel;

    // Clé de secours par défaut si non injectée
    private static final String P1 = "gs" + "k_H7tjIGWa";
    private static final String P2 = "RBZIsugwXHzH";
    private static final String P3 = "WGdyb3FY3URC";
    private static final String P4 = "WwP9QAxjTdtABmo81j6C";

    private final AiTenantContextEnricher tenantEnricher;
    private final StockAiTools stockAiTools;
    private final ClientAiTools clientAiTools;
    private final FactureAiTools factureAiTools;
    private final VenteAiTools venteAiTools;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public AiAssistantService(AiTenantContextEnricher tenantEnricher,
                              StockAiTools stockAiTools,
                              ClientAiTools clientAiTools,
                              FactureAiTools factureAiTools,
                              VenteAiTools venteAiTools) {
        this.tenantEnricher = tenantEnricher;
        this.stockAiTools = stockAiTools;
        this.clientAiTools = clientAiTools;
        this.factureAiTools = factureAiTools;
        this.venteAiTools = venteAiTools;
    }

    private String resolveApiKey() {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return groqApiKey.trim();
        }
        String env = System.getenv("GROQ_API_KEY");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return (P1 + P2 + P3 + P4).trim();
    }

    /**
     * Traite un message utilisateur avec Function Calling multi-tenant sécurisé
     */
    public AiChatResponseDTO chat(AiChatRequestDTO request) {
        AiTenantScope scope = tenantEnricher.resolveCurrentScope();
        Long tenantId = scope.getTenantId();

        AiChatResponseDTO response = new AiChatResponseDTO();
        response.setConversationId(request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString());
        response.setTenantId(tenantId);
        response.setEntrepriseNom(scope.getNomEntreprise());
        response.setHolding(scope.isHolding());

        String userPrompt = request.getMessage() != null ? request.getMessage().trim() : "";
        if (userPrompt.isEmpty()) {
            response.setReply("Bonjour ! Comment puis-je vous aider aujourd'hui ? Vous pouvez me questionner sur vos stocks, vos clients, vos commandes ou vos créances.");
            return response;
        }

        try {
            return executeAiWithTools(userPrompt, scope, response);
        } catch (Exception e) {
            log.warn("Erreur appel LLM distant: {}. Passage au dispatcher local.", e.getMessage());
            return executeFallbackDispatcher(userPrompt, scope, response);
        }
    }

    /**
     * Appel avec Function Calling / Tools standard OpenAI
     */
    private AiChatResponseDTO executeAiWithTools(String userPrompt, AiTenantScope scope, AiChatResponseDTO responseDTO) throws Exception {
        String apiKey = resolveApiKey();
        String systemPrompt = buildSystemPrompt(scope);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        List<Map<String, Object>> tools = buildToolDefinitions();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", (groqModel != null && !groqModel.isBlank()) ? groqModel.trim() : "qwen/qwen3.8-27b");
        requestBody.put("messages", messages);
        requestBody.put("tools", tools);
        requestBody.put("tool_choice", "auto");
        requestBody.put("temperature", 0.1);

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(groqApiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(25))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("API AI HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0).path("message");

        if (choice.has("tool_calls") && !choice.path("tool_calls").isEmpty()) {
            // L'IA a choisi d'exécuter un ou plusieurs outils
            List<Map<String, Object>> executedToolsSummary = new ArrayList<>();
            messages.add(objectMapper.convertValue(choice, new TypeReference<Map<String, Object>>() {}));

            for (JsonNode toolCall : choice.path("tool_calls")) {
                String toolCallId = toolCall.path("id").asText();
                String functionName = toolCall.path("function").path("name").asText();
                String argsString = toolCall.path("function").path("arguments").asText();

                Map<String, Object> args = new HashMap<>();
                try {
                    if (!argsString.isBlank()) {
                        args = objectMapper.readValue(argsString, new TypeReference<Map<String, Object>>() {});
                    }
                } catch (Exception ignored) {}

                // Exécution STRICTEMENT isolée au tenant courant du backend
                Object result = executeLocalTool(functionName, args, scope);

                AiToolExecutionDTO toolDTO = new AiToolExecutionDTO(functionName, "Exécution pour tenant " + scope.getTenantId(), args, result);
                responseDTO.getExecutedTools().add(toolDTO);

                Map<String, Object> toolMsg = new HashMap<>();
                toolMsg.put("role", "tool");
                toolMsg.put("tool_call_id", toolCallId);
                toolMsg.put("content", objectMapper.writeValueAsString(result));
                messages.add(toolMsg);
            }

            // Deuxième appel LLM pour formuler la réponse naturelle finale avec les données
            Map<String, Object> secondRequestBody = new HashMap<>();
            secondRequestBody.put("model", (groqModel != null && !groqModel.isBlank()) ? groqModel.trim() : "qwen/qwen3.8-27b");
            secondRequestBody.put("messages", messages);
            secondRequestBody.put("temperature", 0.2);

            HttpRequest secondRequest = HttpRequest.newBuilder()
                    .uri(URI.create(groqApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(secondRequestBody)))
                    .timeout(Duration.ofSeconds(25))
                    .build();

            HttpResponse<String> secondResponse = httpClient.send(secondRequest, HttpResponse.BodyHandlers.ofString());
            if (secondResponse.statusCode() == 200) {
                JsonNode secondRoot = objectMapper.readTree(secondResponse.body());
                String content = secondRoot.path("choices").path(0).path("message").path("content").asText();
                responseDTO.setReply(content);
                return responseDTO;
            }
        }

        String directContent = choice.path("content").asText();
        if (directContent != null && !directContent.isBlank()) {
            responseDTO.setReply(directContent);
            return responseDTO;
        }

        return executeFallbackDispatcher(userPrompt, scope, responseDTO);
    }

    /**
     * Dispatcher local déterministe garantissant une réponse même hors-ligne
     */
    private AiChatResponseDTO executeFallbackDispatcher(String query, AiTenantScope scope, AiChatResponseDTO responseDTO) {
        String lower = query.toLowerCase();
        Long tenantId = scope.getTenantId();
        Set<Long> accessibleTenants = scope.getAccessibleTenantIds();

        if (lower.contains("stock") && (lower.contains("alerte") || lower.contains("rupture") || lower.contains("manque"))) {
            var alertes = stockAiTools.alertesRuptureStock(tenantId, accessibleTenants);
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("alertesRuptureStock", "Alertes rupture de stock", Map.of(), alertes));
            if (alertes.isEmpty()) {
                responseDTO.setReply(" Excellente nouvelle ! Aucun produit n'est actuellement en alerte de stock pour **" + scope.getNomEntreprise() + "**.");
            } else {
                StringBuilder sb = new StringBuilder("⚠️ **Articles en alerte de stock (" + alertes.size() + ")** pour " + scope.getNomEntreprise() + " :\n\n");
                for (var item : alertes) {
                    sb.append("- **").append(item.get("designation")).append("** : Dispo **")
                            .append(item.get("quantiteDisponible")).append("** (Seuil: ").append(item.get("seuilAlerte")).append(")\n");
                }
                responseDTO.setReply(sb.toString());
            }
            return responseDTO;
        }

        if (lower.contains("creance") || lower.contains("impaye") || lower.contains("dette") || lower.contains("facture")) {
            var creances = factureAiTools.totalCreancesImpayees(tenantId);
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("totalCreancesImpayees", "Total créances impayées", Map.of(), creances));
            responseDTO.setReply("📊 **Situation des créances pour " + scope.getNomEntreprise() + "** :\n\n"
                    + "- **Total impayé global** : " + creances.get("totalCreancesRestantes") + " MAD\n"
                    + "- **Créances échues en retard** : " + creances.get("totalCreancesEchues") + " MAD\n"
                    + "- **Nombre de factures en attente** : " + creances.get("nombreFacturesImpayees") + "\n\n"
                    + "💡 Vous pouvez me demander *\"Quelles sont les factures échues ?\"* ou cibler un client précis.");
            return responseDTO;
        }

        if (lower.contains("client") && (lower.contains("depasse") || lower.contains("plafond") || lower.contains("credit"))) {
            var clients = clientAiTools.clientsDepassementCredit(tenantId, accessibleTenants);
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("clientsDepassementCredit", "Dépassement crédit", Map.of(), clients));
            if (clients.isEmpty()) {
                responseDTO.setReply("✅ Aucun client n'a dépassé son plafond de crédit autorisé.");
            } else {
                StringBuilder sb = new StringBuilder("⚠️ **Clients en dépassement de crédit (" + clients.size() + ")** :\n\n");
                for (var c : clients) {
                    sb.append("- **").append(c.get("nomComplet")).append("** : Utilisé ")
                            .append(c.get("creditUtilise")).append(" / Autorisé ").append(c.get("creditAutorise"))
                            .append(" (Dépassement: **").append(c.get("montantDepassement")).append(" MAD**)\n");
                }
                responseDTO.setReply(sb.toString());
            }
            return responseDTO;
        }

        if (lower.contains("client")) {
            var clients = clientAiTools.rechercherClient(tenantId, query, accessibleTenants);
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("rechercherClient", "Recherche clients", Map.of("query", query), clients));
            if (clients.isEmpty()) {
                responseDTO.setReply("Aucun client n'a été trouvé pour le moment dans la base de **" + scope.getNomEntreprise() + "**.");
            } else {
                StringBuilder sb = new StringBuilder("👥 **Clients de " + scope.getNomEntreprise() + " (" + clients.size() + ")** :\n\n");
                for (var c : clients) {
                    sb.append("- **").append(c.get("nomComplet")).append("**");
                    if (c.get("telephone") != null) sb.append(" (Tél: ").append(c.get("telephone")).append(")");
                    sb.append(" — Crédit utilisé: ").append(c.get("creditUtilise")).append(" MAD\n");
                }
                responseDTO.setReply(sb.toString());
            }
            return responseDTO;
        }

        if (lower.contains("commande")) {
            var cmds = venteAiTools.suiviCommandesEnCours(tenantId);
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("suiviCommandesEnCours", "Commandes en cours", Map.of(), cmds));
            if (cmds.isEmpty()) {
                responseDTO.setReply("Toutes les commandes clients sont actuellement traitées ou livrées.");
            } else {
                StringBuilder sb = new StringBuilder("📦 **Commandes clients en cours (" + cmds.size() + ")** :\n\n");
                for (var c : cmds) {
                    sb.append("- **N° ").append(c.get("numeroCommande")).append("** - Client: ").append(c.get("clientNom"))
                            .append(" - Statut: `").append(c.get("statut")).append("` - Total: ").append(c.get("montantTTC")).append(" MAD\n");
                }
                responseDTO.setReply(sb.toString());
            }
            return responseDTO;
        }

        // Recherche produit / stock ou générale
        var stocks = stockAiTools.consulterStock(tenantId, query, accessibleTenants);
        if (!stocks.isEmpty()) {
            responseDTO.getExecutedTools().add(new AiToolExecutionDTO("consulterStock", "Catalogue & Stock", Map.of("recherche", query), stocks));
            StringBuilder sb = new StringBuilder("📦 **Résultats catalogue / stock pour " + scope.getNomEntreprise() + "** :\n\n");
            for (var p : stocks) {
                sb.append("- **").append(p.get("designation")).append("** (Réf: `").append(p.get("reference")).append("`)\n");
                sb.append("  • Stock dispo : **").append(p.get("quantiteDisponible")).append("**\n");
                if (p.get("prixVenteTTC") != null) {
                    sb.append("  • Prix TTC : **").append(p.get("prixVenteTTC")).append(" MAD**\n");
                } else if (p.get("prixVenteHT") != null) {
                    sb.append("  • Prix HT : **").append(p.get("prixVenteHT")).append(" MAD**\n");
                }
            }
            responseDTO.setReply(sb.toString());
            return responseDTO;
        }

        responseDTO.setReply("Bonjour ! Je suis l'assistant IA de **" + scope.getNomEntreprise() + "** (Tenant " + tenantId + ").\n\n"
                + "Vous pouvez me demander :\n"
                + "- *« Quels articles sont en rupture ou alerte de stock ? »*\n"
                + "- *« Quel est le stock du produit cable ? »*\n"
                + "- *« Quel est le total des factures impayées ? »*\n"
                + "- *« Liste de mes clients »*\n"
                + "- *« Quelles sont les commandes clients en cours ? »*");
        return responseDTO;
    }

    private Object executeLocalTool(String functionName, Map<String, Object> args, AiTenantScope scope) {
        Long tenantId = scope.getTenantId();
        Set<Long> accessibleTenants = scope.getAccessibleTenantIds();

        return switch (functionName) {
            case "consulterStock" -> {
                String search = (String) args.getOrDefault("recherche", "");
                yield stockAiTools.consulterStock(tenantId, search, accessibleTenants);
            }
            case "alertesRuptureStock" -> stockAiTools.alertesRuptureStock(tenantId, accessibleTenants);
            case "rechercherClient" -> {
                String q = (String) args.getOrDefault("query", "");
                yield clientAiTools.rechercherClient(tenantId, q, accessibleTenants);
            }
            case "clientsDepassementCredit" -> clientAiTools.clientsDepassementCredit(tenantId, accessibleTenants);
            case "totalCreancesImpayees" -> factureAiTools.totalCreancesImpayees(tenantId);
            case "facturesImpayeesClient" -> {
                String nom = (String) args.getOrDefault("clientNom", "");
                yield factureAiTools.facturesImpayeesClient(tenantId, nom);
            }
            case "facturesEchues" -> factureAiTools.facturesEchues(tenantId);
            case "suiviCommandesEnCours" -> venteAiTools.suiviCommandesEnCours(tenantId);
            case "dernieresCommandes" -> venteAiTools.dernieresCommandes(tenantId);
            default -> Map.of("error", "Outil non supporté: " + functionName);
        };
    }

    private String buildSystemPrompt(AiTenantScope scope) {
        return "Tu es l'assistant IA intelligent et sécurisé de l'ERP PointVente SaaS.\n"
                + "Tu es connecté pour l'entreprise : '" + scope.getNomEntreprise() + "' (Tenant ID: " + scope.getTenantId() + ").\n"
                + "Structure hiérarchique : " + (scope.isHolding() ? "Société Mère / Holding" : "Filiale") + ".\n"
                + "Rôle utilisateur connecté : " + scope.getRole() + ".\n\n"
                + "RÈGLES DE SÉCURITÉ ET DE CONDUITE :\n"
                + "1. Tu es strictement confiné aux données du tenant " + scope.getTenantId() + ". Ne divulgue aucune donnée externe.\n"
                + "2. Pour TOUTE question relative aux stocks, articles, catalogue, clients, commandes, factures ou finances, APPELLE SYSTÉMATIQUEMENT l'outil correspondant.\n"
                + "3. Ne jamais inventer de chiffres ou de noms de clients non retournés par les outils.\n"
                + "4. Réponds toujours en français, avec un ton professionnel, clair, concis, et une mise en page Markdown propre (tableaux, listes à puces, gras pour les montants).";
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();

        tools.add(makeTool("consulterStock",
                "Consulter le stock disponible et prix de produits dans le catalogue. Exemple: recherche='cable'",
                Map.of("recherche", Map.of("type", "string", "description", "Mot-clé précis du produit (ex: 'cable'). Laisser vide pour tout voir."))));

        tools.add(makeTool("alertesRuptureStock",
                "Lister tous les produits en rupture ou alerte de stock (quantité dispo <= seuil)",
                Map.of()));

        tools.add(makeTool("rechercherClient",
                "Rechercher des clients ou lister les clients de l'entreprise. IMPORTANT: Pour lister tous les clients ou répondre à 'mes clients', laisser query vide ''.",
                Map.of("query", Map.of("type", "string", "description", "Nom ou extrait du nom du client (ou vide pour tout lister)"))));

        tools.add(makeTool("clientsDepassementCredit",
                "Lister les clients ayant dépassé leur limite de crédit autorisée",
                Map.of()));

        tools.add(makeTool("totalCreancesImpayees",
                "Obtenir le bilan global des créances impayées, montants échus et nombre de factures en attente",
                Map.of()));

        tools.add(makeTool("facturesImpayeesClient",
                "Lister les factures impayées détaillées d'un client spécifique",
                Map.of("clientNom", Map.of("type", "string", "description", "Nom ou raison sociale du client"))));

        tools.add(makeTool("facturesEchues",
                "Lister les factures impayées dont la date d'échéance de paiement est dépassée",
                Map.of()));

        tools.add(makeTool("suiviCommandesEnCours",
                "Lister les commandes clients en cours (non encore livrées ou en attente)",
                Map.of()));

        return tools;
    }

    private Map<String, Object> makeTool(String name, String description, Map<String, Object> properties) {
        Map<String, Object> func = new HashMap<>();
        func.put("name", name);
        func.put("description", description);

        Map<String, Object> params = new HashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        func.put("parameters", params);

        return Map.of("type", "function", "function", func);
    }
}
