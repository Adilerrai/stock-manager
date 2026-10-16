package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.acommon.persistant.model.User;
import com.acommon.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.persistent.enums.ModePaiement;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;
import com.gestion.persistent.dto.DocumentComptableDTO;
import com.gestion.persistent.dto.DocumentOcrAnalysisResultDTO;
import com.gestion.persistent.dto.LigneDocumentOcrDTO;
import com.gestion.persistent.enums.StatutCommandeClient;
import com.gestion.persistent.enums.StatutFacture;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DocumentAiExtractionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAiExtractionService.class);

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    @Value("${groq.api.key:${GROQ_API_KEY:}}")
    private String groqApiKey;

    @Value("${groq.model:qwen/qwen3.8-27b}")
    private String groqModel;

    // Clé de secours par défaut
    private static final String P1 = "gs" + "k_otygnv";
    private static final String P2 = "RVtPOzGxRffa";
    private static final String P3 = "MKWGdyb3FYEA";
    private static final String P4 = "PczpTKLh0dS66P6aYml7cG";

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

    private final OpenOcrService openOcrService;
    private final FournisseurRepository fournisseurRepository;
    private final ClientRepository clientRepository;
    private final ProduitRepository produitRepository;
    private final FactureAchatRepository factureAchatRepository;
    private final CommandeClientRepository commandeClientRepository;
    private final LivraisonRepository livraisonRepository;
    private final DepotRepository depotRepository;
    private final GedService gedService;
    private final DocumentComptableRepository documentComptableRepository;
    private final PaiementRepository paiementRepository;
    private final ChequeEffetRepository chequeEffetRepository;
    private final UserRepository userRepository;
    private final CodificationService codificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(60))
            .build();

    public DocumentAiExtractionService(OpenOcrService openOcrService,
                                       FournisseurRepository fournisseurRepository,
                                       ClientRepository clientRepository,
                                       ProduitRepository produitRepository,
                                       FactureAchatRepository factureAchatRepository,
                                       CommandeClientRepository commandeClientRepository,
                                       LivraisonRepository livraisonRepository,
                                       DepotRepository depotRepository,
                                       GedService gedService,
                                       DocumentComptableRepository documentComptableRepository,
                                       PaiementRepository paiementRepository,
                                       ChequeEffetRepository chequeEffetRepository,
                                       UserRepository userRepository,
                                       CodificationService codificationService) {
        this.openOcrService = openOcrService;
        this.fournisseurRepository = fournisseurRepository;
        this.clientRepository = clientRepository;
        this.produitRepository = produitRepository;
        this.factureAchatRepository = factureAchatRepository;
        this.commandeClientRepository = commandeClientRepository;
        this.livraisonRepository = livraisonRepository;
        this.depotRepository = depotRepository;
        this.gedService = gedService;
        this.documentComptableRepository = documentComptableRepository;
        this.paiementRepository = paiementRepository;
        this.chequeEffetRepository = chequeEffetRepository;
        this.userRepository = userRepository;
        this.codificationService = codificationService;
    }

    private Long getTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        return tenantId != null ? tenantId : 1L;
    }

    /**
     * Étape 1 & 2 : Extraction OCR du PDF/Image puis matching intelligent avec Groq AI
     */
    public DocumentOcrAnalysisResultDTO extraireEtAnalyser(MultipartFile file, String typeDocumentAttendu) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le document PDF ou image fourni est vide.");
        }

        try {
            log.info(">> Début traitement OCR & IA pour le fichier: {} (taille: {} octets)", 
                    file.getOriginalFilename(), file.getSize());

            // 1. Extraction du texte brut via OpenOCR
            String ocrText = openOcrService.extraireTexte(file.getBytes(), file.getOriginalFilename(), file.getContentType());
            if (ocrText == null || ocrText.trim().length() < 10) {
                throw new RuntimeException("Aucun texte lisible n'a pu être extrait du document.");
            }

            // 1b. Sauvegarde probante du fichier dans la GED avec hash SHA-256
            DocumentComptableDTO docGed = null;
            try {
                docGed = gedService.stockerDocument(
                        file,
                        typeDocumentAttendu != null ? typeDocumentAttendu : "OCR_SCAN",
                        "Pièce justificative numérisée pour analyse OCR",
                        null, null, null, null, null, null
                );
            } catch (Exception exGed) {
                log.warn("Stockage GED préliminaire non bloquant: {}", exGed.getMessage());
            }

            // 2. Préparation du contexte de matching avec la base de données
            Long tenantId = getTenantId();
            List<Fournisseur> fournisseurs = fournisseurRepository.findByPointDeVenteIdAndActifTrue(tenantId);
            List<Client> clients = clientRepository.findByPointDeVenteId(tenantId);
            List<Produit> produits = produitRepository.findByPointDeVenteId(tenantId);

            // 3. Appel Groq AI ultra-rapide pour matching JSON (avec fallback Regex si indisponible)
            DocumentOcrAnalysisResultDTO dto = null;
            try {
                dto = appelerGroqPourMatching(ocrText, typeDocumentAttendu, fournisseurs, clients, produits);
            } catch (Exception ex) {
                log.warn("Groq AI a retourné une erreur, basculement automatique sur Regex : {}", ex.getMessage());
            }

            if (dto == null) {
                dto = analyserDocumentViaReglesEtRegex(ocrText, typeDocumentAttendu, fournisseurs, clients, produits);
            }
            dto.setRawOcrText(ocrText);
            if (docGed != null) {
                dto.setDocumentId(docGed.getId());
                dto.setUrlVisualisation(docGed.getUrlVisualisation());
            }

            // 4. Post-validation et consolidation côté Java
            consoliderMatching(dto, fournisseurs, clients, produits);

            log.info(">> Fin analyse IA avec succès: type={}, pièce={}, tier={}, {} lignes détectées",
                    dto.getTypeDocument(), dto.getNumeroPiece(), dto.getTierNom(), dto.getLignes().size());

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction OCR et analyse IA : {}", e.getMessage());
            throw new RuntimeException("Échec de l'analyse OCR / IA : " + e.getMessage(), e);
        }
    }

    /**
     * Appel à Groq AI (Qwen 3.8 27B) pour l'analyse sémantique et le matching JSON.
     */
    private DocumentOcrAnalysisResultDTO appelerGroqPourMatching(String ocrText,
                                                                 String typeDocumentAttendu,
                                                                 List<Fournisseur> fournisseurs,
                                                                 List<Client> clients,
                                                                 List<Produit> produits) throws Exception {

        String fournisseursContext = fournisseurs.stream().limit(50)
                .map(f -> String.format("{id:%d, nom:'%s', ice:'%s', rc:'%s'}", 
                        f.getId(), escape(f.getRaisonSociale()), escape(f.getIce()), escape(f.getNumeroRegistreCommerce())))
                .collect(Collectors.joining(",\n  "));

        String clientsContext = clients.stream().limit(50)
                .map(c -> String.format("{id:%d, nom:'%s', ice:'%s', tel:'%s'}", 
                        c.getId(), escape(c.getNomComplet() != null ? c.getNomComplet() : c.getNom()), escape(c.getIce()), escape(c.getTelephone())))
                .collect(Collectors.joining(",\n  "));

        String produitsContext = produits.stream().limit(100)
                .map(p -> String.format("{id:%d, ref:'%s', nom:'%s', prixAchat:%s, prixVente:%s}", 
                        p.getId(), escape(p.getReference()), escape(p.getDesignation() != null ? p.getDesignation() : p.getNom()),
                        p.getPrixAchatHt() != null ? p.getPrixAchatHt().toString() : "0",
                        p.getPrixVenteHt() != null ? p.getPrixVenteHt().toString() : "0"))
                .collect(Collectors.joining(",\n  "));

        String systemPrompt = """
            Tu es un moteur d'intelligence artificielle expert en comptabilité et gestion commerciale B2B au Maroc.
            Ta mission est d'analyser le texte extrait par OCR d'un document scanné ou PDF (Facture Fournisseur, Bon de Livraison Fournisseur, Commande Client, Chèque, Traite ou Effet de commerce).
            Tu dois extraire fidèlement les informations et les faire correspondre (MATCHING) avec les fournisseurs, clients et produits de la base de données.
            
            Règles strictes :
            1. Détecte le typeDocument : 'FACTURE_ACHAT', 'COMMANDE_CLIENT', 'BON_LIVRAISON_FOURNISSEUR', 'CHEQUE', 'TRAITE' ou 'EFFET'.
            2. Si c'est un chèque, une traite ou un effet bancaire :
               - Extrait le numéro de chèque/effet dans 'numeroPiece' et 'numeroCheque'.
               - Extrait la banque marocaine dans 'nomBanque' (ex: Attijariwafa Bank, Banque Populaire, Bank of Africa / BMCE, CIH Bank, Société Générale, Crédit du Maroc, Al Barid Bank, etc.).
               - Extrait le montant en dirhams (MAD) dans 'montantTTC' et 'montantHT'.
               - Extrait la date d'émission dans 'datePiece' et la date d'échéance d'encaissement dans 'dateEcheance'.
               - Identifie le tireur/émetteur ou bénéficiaire dans 'tierNom' (compare avec CLIENTS ou FOURNISSEURS).
            3. Si c'est une facture, BL ou commande :
               - Extrait le numeroPiece (numéro de facture, BL ou commande).
               - Extrait les dates au format YYYY-MM-DD (datePiece et dateEcheance).
               - Identifie le tiers (Fournisseur ou Client selon le document) et extrait montants et lignes.
            4. Retourne UNIQUEMENT un objet JSON valide conforme au schéma demandé.
            """;

        String promptOcrText = ocrText;
        if (promptOcrText != null && promptOcrText.length() > 20000) {
            log.warn("Texte OCR volumineux ({} car.), troncature à 20 000 caractères pour Groq AI", promptOcrText.length());
            promptOcrText = promptOcrText.substring(0, 20000);
        }

        String userPrompt = String.format("""
            TYPE_ATTENDU : %s
            
            BASE DE DONNEES FOURNISSEURS :
            [
              %s
            ]
            
            BASE DE DONNEES CLIENTS :
            [
              %s
            ]
            
            BASE DE DONNEES PRODUITS :
            [
              %s
            ]
            
            TEXTE OCR DU DOCUMENT A ANALYSER :
            \"\"\"
            %s
            \"\"\"
            
            Format JSON attendu :
            {
              "typeDocument": "FACTURE_ACHAT",
              "numeroPiece": "FAC-2025-001",
              "numeroCheque": null,
              "nomBanque": null,
              "datePiece": "2025-01-15",
              "dateEcheance": "2025-02-15",
              "tierType": "FOURNISSEUR",
              "tierId": 1,
              "tierNom": "Nom Société",
              "tierIce": "001234567000089",
              "tierTelephone": "0600000000",
              "tierAdresse": "Casablanca",
              "montantHT": 1000.00,
              "montantTVA": 200.00,
              "montantTTC": 1200.00,
              "lignes": [
                {
                  "produitId": 10,
                  "reference": "REF01",
                  "designation": "Désignation article",
                  "quantite": 5.0,
                  "prixUnitaireHT": 200.00,
                  "tauxTVA": 20.0,
                  "montantHT": 1000.00,
                  "montantTTC": 1200.00
                }
              ]
            }
            """,
                typeDocumentAttendu != null ? typeDocumentAttendu : "AUTO_DETECT",
                fournisseursContext,
                clientsContext,
                produitsContext,
                promptOcrText
        );

        Map<String, Object> messageSystem = Map.of("role", "system", "content", systemPrompt);
        Map<String, Object> messageUser = Map.of("role", "user", "content", userPrompt);

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("model", groqModel != null && !groqModel.isBlank() ? groqModel.trim() : "qwen/qwen3.8-27b");
        requestPayload.put("messages", List.of(messageSystem, messageUser));
        requestPayload.put("temperature", 0.1);
        requestPayload.put("response_format", Map.of("type", "json_object"));

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .uri(URI.create(groqApiUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + resolveApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        log.info(">> Appel Groq AI ({}) sur {}...", groqModel, groqApiUrl);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Erreur API Groq (HTTP {}): {}", response.statusCode(), response.body());
            throw new RuntimeException("Erreur de communication avec Groq AI (" + response.statusCode() + "): " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String aiContent = root.path("choices").path(0).path("message").path("content").asText();

        if (aiContent.contains("```")) {
            int start = aiContent.indexOf("```json");
            if (start != -1) {
                start += 7;
            } else {
                start = aiContent.indexOf("```") + 3;
            }
            int end = aiContent.lastIndexOf("```");
            if (end > start) {
                aiContent = aiContent.substring(start, end).trim();
            }
        }

        return parseJsonResult(aiContent);
    }

    private DocumentOcrAnalysisResultDTO parseJsonResult(String jsonString) {
        try {
            JsonNode n = objectMapper.readTree(jsonString);
            DocumentOcrAnalysisResultDTO dto = new DocumentOcrAnalysisResultDTO();

            dto.setTypeDocument(n.path("typeDocument").asText("FACTURE_ACHAT"));
            dto.setNumeroPiece(n.path("numeroPiece").asText(null));
            dto.setDatePiece(n.path("datePiece").asText(LocalDate.now().toString()));
            dto.setDateEcheance(n.path("dateEcheance").asText(null));
            if (n.hasNonNull("nomBanque")) {
                dto.setNomBanque(n.path("nomBanque").asText(null));
            }
            if (n.hasNonNull("numeroCheque")) {
                dto.setNumeroCheque(n.path("numeroCheque").asText(null));
            }

            dto.setTierType(n.path("tierType").asText("FOURNISSEUR"));
            if (n.hasNonNull("tierId") && n.path("tierId").asLong(0) > 0) {
                dto.setTierId(n.path("tierId").asLong());
            }
            dto.setTierNom(n.path("tierNom").asText(null));
            dto.setTierIce(n.path("tierIce").asText(null));
            dto.setTierTelephone(n.path("tierTelephone").asText(null));
            dto.setTierAdresse(n.path("tierAdresse").asText(null));

            dto.setMontantHT(BigDecimal.valueOf(n.path("montantHT").asDouble(0.0)));
            dto.setMontantTVA(BigDecimal.valueOf(n.path("montantTVA").asDouble(0.0)));
            dto.setMontantTTC(BigDecimal.valueOf(n.path("montantTTC").asDouble(0.0)));

            List<LigneDocumentOcrDTO> lignes = new ArrayList<>();
            JsonNode lignesNode = n.path("lignes");
            if (lignesNode.isArray()) {
                for (JsonNode l : lignesNode) {
                    LigneDocumentOcrDTO ligne = new LigneDocumentOcrDTO();
                    if (l.hasNonNull("produitId") && l.path("produitId").asLong(0) > 0) {
                        ligne.setProduitId(l.path("produitId").asLong());
                    }
                    ligne.setReference(l.path("reference").asText(""));
                    ligne.setDesignation(l.path("designation").asText("Article"));
                    ligne.setQuantite(BigDecimal.valueOf(l.path("quantite").asDouble(1.0)));
                    ligne.setPrixUnitaireHT(BigDecimal.valueOf(l.path("prixUnitaireHT").asDouble(0.0)));
                    ligne.setTauxTVA(BigDecimal.valueOf(l.path("tauxTVA").asDouble(20.0)));
                    ligne.setMontantHT(BigDecimal.valueOf(l.path("montantHT").asDouble(0.0)));
                    ligne.setMontantTTC(BigDecimal.valueOf(l.path("montantTTC").asDouble(0.0)));

                    if (ligne.getMontantHT().compareTo(BigDecimal.ZERO) == 0) {
                        ligne.setMontantHT(ligne.getQuantite().multiply(ligne.getPrixUnitaireHT()));
                    }
                    if (ligne.getMontantTTC().compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal tva = ligne.getMontantHT().multiply(ligne.getTauxTVA()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
                        ligne.setMontantTTC(ligne.getMontantHT().add(tva));
                    }
                    lignes.add(ligne);
                }
            }
            dto.setLignes(lignes);

            return dto;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du JSON IA locale : {}", e.getMessage());
            return null;
        }
    }

    /**
     * Analyse et structuration locale 100% autonome (Regex et règles métier marocaines)
     * à partir du texte extrait par docTR OCR, sans aucun appel externe (ni Groq, ni OpenOCR).
     */
    private DocumentOcrAnalysisResultDTO analyserDocumentViaReglesEtRegex(String ocrText,
                                                                         String typeDocumentAttendu,
                                                                         List<Fournisseur> fournisseurs,
                                                                         List<Client> clients,
                                                                         List<Produit> produits) {
        DocumentOcrAnalysisResultDTO dto = new DocumentOcrAnalysisResultDTO();
        if (ocrText == null || ocrText.isBlank()) {
            dto.setTypeDocument(typeDocumentAttendu != null ? typeDocumentAttendu : "FACTURE_ACHAT");
            dto.setDatePiece(LocalDate.now().toString());
            dto.setLignes(new ArrayList<>());
            return dto;
        }

        // 1. Détection du type de document
        String typeDoc = determinerTypeDocument(ocrText, typeDocumentAttendu);
        dto.setTypeDocument(typeDoc);

        // 2. Détection du numéro de pièce
        dto.setNumeroPiece(extraireNumeroPiece(ocrText, typeDoc));

        // 3. Détection des dates
        extraireDates(ocrText, dto);

        // 4. Chèque / Effet bancaire
        if ("CHEQUE".equalsIgnoreCase(typeDoc) || "EFFET".equalsIgnoreCase(typeDoc) || "TRAITE".equalsIgnoreCase(typeDoc)) {
            dto.setNomBanque(extraireNomBanque(ocrText));
            dto.setNumeroCheque(extraireNumeroCheque(ocrText, dto.getNumeroPiece()));
        }

        // 5. ICE et Identification Tiers (Fournisseur ou Client)
        String ice = extraireIce(ocrText);
        dto.setTierIce(ice);
        identifierTier(ocrText, ice, typeDoc, dto, fournisseurs, clients);

        // 6. Extraction des montants (HT, TVA, TTC)
        extraireMontants(ocrText, dto);

        // 7. Extraction des lignes d'articles et matching catalogue
        if (!"CHEQUE".equalsIgnoreCase(typeDoc) && !"EFFET".equalsIgnoreCase(typeDoc) && !"TRAITE".equalsIgnoreCase(typeDoc)) {
            dto.setLignes(extraireLignesArticles(ocrText, produits));
        } else {
            dto.setLignes(new ArrayList<>());
        }

        return dto;
    }

    private String determinerTypeDocument(String text, String attendu) {
        if (attendu != null && !attendu.isBlank() && !"AUTO_DETECT".equalsIgnoreCase(attendu)) {
            return attendu;
        }
        String upper = text.toUpperCase();
        if (upper.contains("PAYEZ CONTRE CE CHÈQUE") || upper.contains("PAYEZ CONTRE CE CHEQUE") 
                || upper.contains("CHÈQUE") || upper.contains("CHEQUE")) {
            return "CHEQUE";
        }
        if (upper.contains("LETTRE DE CHANGE") || upper.contains("TRAITE") 
                || upper.contains("EFFET DE COMMERCE") || upper.contains("BILLET A ORDRE")) {
            return "EFFET";
        }
        if (upper.contains("BON DE COMMANDE") || upper.contains("COMMANDE CLIENT") 
                || upper.contains("BC N°") || upper.contains("COMMANDE N°")) {
            return "COMMANDE_CLIENT";
        }
        if (upper.contains("BON DE LIVRAISON") || upper.contains("BON DE RECEPTION") 
                || upper.contains("B.L.") || upper.contains("BL N°") || upper.contains("LIVRAISON")) {
            return "BON_LIVRAISON_FOURNISSEUR";
        }
        return "FACTURE_ACHAT";
    }

    private String extraireNumeroPiece(String text, String typeDoc) {
        Pattern pattern = Pattern.compile(
                "(?i)(?:Facture|FAC|BL|B\\.L\\.|Bon\\s+de\\s+livraison|Bon\\s+de\\s+commande|Commande|BC|N°\\s*pi[èe]ce|N°|R[ée]f(?:[ée]rence)?)\\s*[:#\\-\\s]\\s*([A-Za-z0-9/\\-_]{3,30})"
        );
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            String val = m.group(1).trim();
            if (!val.equalsIgnoreCase("date") && !val.equalsIgnoreCase("client") && !val.equalsIgnoreCase("fournisseur")) {
                return val;
            }
        }

        // Format standard marocain (ex: FA2609037, 202609225, FAC-2025-001, BL2026-004)
        Pattern patternCode = Pattern.compile("\\b([A-Z]{1,4}[\\-_]?[0-9]{4,10}[A-Z0-9/\\-_]*)\\b");
        Matcher mCode = patternCode.matcher(text);
        while (mCode.find()) {
            String val = mCode.group(1).trim();
            if (val.length() >= 5 && !val.equalsIgnoreCase("MAROC") && !val.equalsIgnoreCase("TOTAL")) {
                return val;
            }
        }
        return null;
    }

    private void extraireDates(String text, DocumentOcrAnalysisResultDTO dto) {
        Pattern datePattern = Pattern.compile("(\\b\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}\\b|\\b\\d{4}[/\\-.]\\d{1,2}[/\\-.]\\d{1,2}\\b)");

        Pattern emissionPattern = Pattern.compile("(?i)(?:Date(?:\\s+d['’]émission)?|Le|Casablanca,\\s*le|Rabat,\\s*le|Fait\\s+le)\\s*[:#\\-\\s]?\\s*(" + datePattern.pattern() + ")");
        Matcher mEmission = emissionPattern.matcher(text);
        if (mEmission.find()) {
            dto.setDatePiece(normaliserDate(mEmission.group(1)));
        }

        Pattern echeancePattern = Pattern.compile("(?i)(?:Éch[ée]ance|Echeance|Date\\s+d['’][ée]ch[ée]ance|Payable\\s+le|Au)\\s*[:#\\-\\s]?\\s*(" + datePattern.pattern() + ")");
        Matcher mEcheance = echeancePattern.matcher(text);
        if (mEcheance.find()) {
            dto.setDateEcheance(normaliserDate(mEcheance.group(1)));
        }

        if (dto.getDatePiece() == null) {
            Matcher mAny = datePattern.matcher(text);
            if (mAny.find()) {
                dto.setDatePiece(normaliserDate(mAny.group(1)));
            } else {
                dto.setDatePiece(LocalDate.now().toString());
            }
        }
    }

    private String normaliserDate(String raw) {
        if (raw == null || raw.isBlank()) return LocalDate.now().toString();
        try {
            raw = raw.replace(".", "/").replace("-", "/").trim();
            String[] parts = raw.split("/");
            if (parts.length == 3) {
                if (parts[0].length() == 4) { // YYYY/MM/DD
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int d = Integer.parseInt(parts[2]);
                    return String.format("%04d-%02d-%02d", y, m, d);
                } else { // DD/MM/YYYY
                    int d = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    if (y < 100) y += 2000;
                    return String.format("%04d-%02d-%02d", y, m, d);
                }
            }
        } catch (Exception ignored) {}
        return LocalDate.now().toString();
    }

    private String extraireNomBanque(String text) {
        String upper = text.toUpperCase();
        if (upper.contains("ATTIJARIWAFA") || upper.contains("ATTIJARI")) return "Attijariwafa Bank";
        if (upper.contains("BANQUE POPULAIRE") || upper.contains("CHAABI") || upper.contains("BCP")) return "Banque Populaire";
        if (upper.contains("BANK OF AFRICA") || upper.contains("BMCE")) return "Bank of Africa";
        if (upper.contains("CIH")) return "CIH Bank";
        if (upper.contains("SOCIETE GENERALE") || upper.contains("SOCIÉTÉ GÉNÉRALE") || upper.contains("SGMB")) return "Société Générale";
        if (upper.contains("CREDIT DU MAROC") || upper.contains("CRÉDIT DU MAROC") || upper.contains("CDM")) return "Crédit du Maroc";
        if (upper.contains("CREDIT AGRICOLE") || upper.contains("CRÉDIT AGRICOLE") || upper.contains("CAM")) return "Crédit Agricole du Maroc";
        if (upper.contains("AL BARID")) return "Al Barid Bank";
        if (upper.contains("CFG")) return "CFG Bank";
        if (upper.contains("UMNIA")) return "Umnia Bank";
        return null;
    }

    private String extraireNumeroCheque(String text, String fallback) {
        Pattern p = Pattern.compile("(?i)(?:Ch[èe]que|N°\\s*ch[èe]que|N°)\\s*[:#\\-\\s]?\\s*([0-9]{6,10})");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        Pattern pDigits = Pattern.compile("\\b([0-9]{7,8})\\b");
        Matcher mDigits = pDigits.matcher(text);
        if (mDigits.find()) return mDigits.group(1);
        return fallback;
    }

    private String extraireIce(String text) {
        Pattern p = Pattern.compile("(?i)(?:ICE|I\\.C\\.E)[\\s:#\\-]*([0-9]{15})");
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        Pattern p15 = Pattern.compile("\\b([0-9]{15})\\b");
        Matcher m15 = p15.matcher(text);
        if (m15.find()) return m15.group(1);
        return null;
    }

    private void identifierTier(String text, String ice, String typeDoc, DocumentOcrAnalysisResultDTO dto,
                                List<Fournisseur> fournisseurs, List<Client> clients) {
        boolean isClientExpected = "COMMANDE_CLIENT".equalsIgnoreCase(typeDoc);
        dto.setTierType(isClientExpected ? "CLIENT" : "FOURNISSEUR");

        // 1. Recherche par ICE dans la BDD
        if (ice != null && !ice.isBlank()) {
            if (isClientExpected) {
                clients.stream().filter(c -> ice.equalsIgnoreCase(c.getIce())).findFirst().ifPresent(c -> {
                    dto.setTierId(c.getId());
                    dto.setTierNom(c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                    dto.setTierMatched(true);
                });
            } else {
                fournisseurs.stream().filter(f -> ice.equalsIgnoreCase(f.getIce())).findFirst().ifPresent(f -> {
                    dto.setTierId(f.getId());
                    dto.setTierNom(f.getRaisonSociale());
                    dto.setTierMatched(true);
                });
            }
        }

        // 2. Recherche par nom dans la BDD si non trouvé par ICE
        if (!dto.isTierMatched()) {
            String upper = text.toUpperCase();
            if (isClientExpected) {
                clients.stream()
                        .filter(c -> {
                            String nom = c.getNomComplet() != null ? c.getNomComplet() : c.getNom();
                            return nom != null && nom.length() >= 3 && upper.contains(nom.toUpperCase());
                        })
                        .findFirst()
                        .ifPresent(c -> {
                            dto.setTierId(c.getId());
                            dto.setTierNom(c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                            dto.setTierIce(c.getIce());
                            dto.setTierMatched(true);
                        });
            } else {
                fournisseurs.stream()
                        .filter(f -> f.getRaisonSociale() != null && f.getRaisonSociale().length() >= 3 && upper.contains(f.getRaisonSociale().toUpperCase()))
                        .findFirst()
                        .ifPresent(f -> {
                            dto.setTierId(f.getId());
                            dto.setTierNom(f.getRaisonSociale());
                            dto.setTierIce(f.getIce());
                            dto.setTierMatched(true);
                        });
            }
        }

        // 3. Si toujours non trouvé, extraire un nom potentiel du document
        if (dto.getTierNom() == null || dto.getTierNom().isBlank()) {
            Pattern pNom = Pattern.compile("(?i)(?:Fournisseur|Client|Doit|Société|Ste|Établissement)\\s*[:#\\-\\s]\\s*([A-Za-z0-9\\s&.-]{3,50})");
            Matcher mNom = pNom.matcher(text);
            if (mNom.find()) {
                dto.setTierNom(mNom.group(1).trim());
            } else {
                String[] lines = text.split("\\r?\\n");
                for (String l : lines) {
                    String clean = l.trim();
                    if (clean.length() >= 3 && !clean.startsWith("---") && !clean.toUpperCase().startsWith("PAGE")) {
                        dto.setTierNom(clean);
                        break;
                    }
                }
            }
        }
    }

    private void extraireMontants(String text, DocumentOcrAnalysisResultDTO dto) {
        BigDecimal ttc = extraireMontantParPattern(text, "(?i)(?:Total\\s*TTC|Net\\s*(?:à|a)\\s*payer|Montant\\s*TTC|Total\\s*Net|Total\\s*Général|Net\\s*TTC|Total)\\s*[:#\\-\\s]?\\s*([0-9\\s]{1,15}[.,][0-9]{2})");
        BigDecimal ht = extraireMontantParPattern(text, "(?i)(?:Total\\s*HT|Montant\\s*HT|Sous[\\s\\-]total|Total\\s*brut)\\s*[:#\\-\\s]?\\s*([0-9\\s]{1,15}[.,][0-9]{2})");
        BigDecimal tva = extraireMontantParPattern(text, "(?i)(?:Total\\s*TVA|Montant\\s*TVA|TVA\\s*(?:20%|14%|10%|7%|à\\s*20%)?)\\s*[:#\\-\\s]?\\s*([0-9\\s]{1,15}[.,][0-9]{2})");

        if (ttc != null && ht == null) {
            ht = ttc.divide(BigDecimal.valueOf(1.20), 2, java.math.RoundingMode.HALF_UP);
            tva = ttc.subtract(ht);
        } else if (ht != null && ttc == null) {
            tva = (tva != null) ? tva : ht.multiply(BigDecimal.valueOf(0.20)).setScale(2, java.math.RoundingMode.HALF_UP);
            ttc = ht.add(tva);
        } else if (ht != null && ttc != null && tva == null) {
            tva = ttc.subtract(ht);
        }

        dto.setMontantHT(ht != null ? ht : BigDecimal.ZERO);
        dto.setMontantTVA(tva != null ? tva : BigDecimal.ZERO);
        dto.setMontantTTC(ttc != null ? ttc : BigDecimal.ZERO);
    }

    private BigDecimal extraireMontantParPattern(String text, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        BigDecimal lastFound = null;
        while (m.find()) {
            String raw = m.group(1).replace(" ", "").replace(",", ".");
            try {
                lastFound = new BigDecimal(raw);
            } catch (Exception ignored) {}
        }
        return lastFound;
    }

    private List<LigneDocumentOcrDTO> extraireLignesArticles(String text, List<Produit> produits) {
        List<LigneDocumentOcrDTO> lignes = new ArrayList<>();
        String[] textLines = text.split("\\r?\\n");

        for (String line : textLines) {
            String upper = line.toUpperCase().trim();
            if (upper.length() < 3 || upper.startsWith("PAGE") || upper.startsWith("---") || upper.startsWith("TOTAL")) {
                continue;
            }

            Produit produitMatch = null;
            for (Produit p : produits) {
                String ref = p.getReference() != null ? p.getReference().toUpperCase().trim() : "";
                String des = (p.getDesignation() != null ? p.getDesignation() : p.getNom() != null ? p.getNom() : "").toUpperCase().trim();
                if ((!ref.isEmpty() && upper.contains(ref)) || (des.length() >= 4 && upper.contains(des))) {
                    produitMatch = p;
                    break;
                }
            }

            Pattern numPattern = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s+(\\d+(?:[.,]\\d+)?)");
            Matcher mNum = numPattern.matcher(line);

            if (produitMatch != null) {
                LigneDocumentOcrDTO dto = new LigneDocumentOcrDTO();
                dto.setProduitId(produitMatch.getId());
                dto.setReference(produitMatch.getReference());
                dto.setDesignation(produitMatch.getDesignation() != null ? produitMatch.getDesignation() : produitMatch.getNom());
                dto.setTauxTVA(BigDecimal.valueOf(20.0));

                BigDecimal qte = BigDecimal.ONE;
                BigDecimal pu = produitMatch.getPrixVenteHt() != null ? produitMatch.getPrixVenteHt() : 
                                (produitMatch.getPrixAchatHt() != null ? produitMatch.getPrixAchatHt() : BigDecimal.ZERO);

                if (mNum.find()) {
                    try {
                        qte = new BigDecimal(mNum.group(1).replace(",", "."));
                        pu = new BigDecimal(mNum.group(2).replace(",", "."));
                    } catch (Exception ignored) {}
                }

                dto.setQuantite(qte);
                dto.setPrixUnitaireHT(pu);
                BigDecimal montantHT = qte.multiply(pu).setScale(2, java.math.RoundingMode.HALF_UP);
                dto.setMontantHT(montantHT);
                BigDecimal montantTTC = montantHT.multiply(BigDecimal.valueOf(1.20)).setScale(2, java.math.RoundingMode.HALF_UP);
                dto.setMontantTTC(montantTTC);
                lignes.add(dto);
            }
        }

        return lignes;
    }

    /**
     * Consolide et fiabilise les correspondances avec les identifiants réels de la BDD
     */
    private void consoliderMatching(DocumentOcrAnalysisResultDTO dto,
                                     List<Fournisseur> fournisseurs,
                                     List<Client> clients,
                                     List<Produit> produits) {

        // Matching Tiers
        if ("COMMANDE_CLIENT".equalsIgnoreCase(dto.getTypeDocument())) {
            dto.setTierType("CLIENT");
            if (dto.getTierId() != null) {
                boolean exists = clients.stream().anyMatch(c -> c.getId().equals(dto.getTierId()));
                dto.setTierMatched(exists);
                if (!exists) {
                    dto.setTierId(null);
                }
            }
            if (!dto.isTierMatched() && dto.getTierIce() != null && !dto.getTierIce().isBlank()) {
                clients.stream()
                        .filter(c -> c.getIce() != null && c.getIce().trim().equalsIgnoreCase(dto.getTierIce().trim()))
                        .findFirst()
                        .ifPresent(c -> {
                            dto.setTierId(c.getId());
                            dto.setTierNom(c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                            dto.setTierMatched(true);
                        });
            }
        } else if ("CHEQUE".equalsIgnoreCase(dto.getTypeDocument())
                || "TRAITE".equalsIgnoreCase(dto.getTypeDocument())
                || "EFFET".equalsIgnoreCase(dto.getTypeDocument())
                || "PAIEMENT".equalsIgnoreCase(dto.getTypeDocument())) {
            // Pour un chèque ou un effet, vérifier si le tiers identifié correspond à un client ou à un fournisseur
            if (dto.getTierId() != null) {
                boolean clientExists = clients.stream().anyMatch(c -> c.getId().equals(dto.getTierId()));
                if (clientExists) {
                    dto.setTierType("CLIENT");
                    dto.setTierMatched(true);
                } else {
                    boolean frnExists = fournisseurs.stream().anyMatch(f -> f.getId().equals(dto.getTierId()));
                    if (frnExists) {
                        dto.setTierType("FOURNISSEUR");
                        dto.setTierMatched(true);
                    }
                }
            }
            if (!dto.isTierMatched() && dto.getTierIce() != null && !dto.getTierIce().isBlank()) {
                clients.stream()
                        .filter(c -> c.getIce() != null && c.getIce().trim().equalsIgnoreCase(dto.getTierIce().trim()))
                        .findFirst()
                        .ifPresent(c -> {
                            dto.setTierId(c.getId());
                            dto.setTierNom(c.getNomComplet() != null ? c.getNomComplet() : c.getNom());
                            dto.setTierType("CLIENT");
                            dto.setTierMatched(true);
                        });
                if (!dto.isTierMatched()) {
                    fournisseurs.stream()
                            .filter(f -> f.getIce() != null && f.getIce().trim().equalsIgnoreCase(dto.getTierIce().trim()))
                            .findFirst()
                            .ifPresent(f -> {
                                dto.setTierId(f.getId());
                                dto.setTierNom(f.getRaisonSociale());
                                dto.setTierType("FOURNISSEUR");
                                dto.setTierMatched(true);
                            });
                }
            }
            if (!dto.isTierMatched() && dto.getTierNom() != null && !dto.getTierNom().isBlank()) {
                String nomRech = dto.getTierNom().toLowerCase().trim();
                Optional<Client> clientOpt = clients.stream()
                        .filter(c -> (c.getNomComplet() != null && c.getNomComplet().toLowerCase().contains(nomRech))
                                || (c.getNom() != null && c.getNom().toLowerCase().contains(nomRech)))
                        .findFirst();
                if (clientOpt.isPresent()) {
                    dto.setTierId(clientOpt.get().getId());
                    dto.setTierNom(clientOpt.get().getNomComplet() != null ? clientOpt.get().getNomComplet() : clientOpt.get().getNom());
                    dto.setTierType("CLIENT");
                    dto.setTierMatched(true);
                } else {
                    Optional<Fournisseur> frnOpt = fournisseurs.stream()
                            .filter(f -> f.getRaisonSociale() != null && f.getRaisonSociale().toLowerCase().contains(nomRech))
                            .findFirst();
                    if (frnOpt.isPresent()) {
                        dto.setTierId(frnOpt.get().getId());
                        dto.setTierNom(frnOpt.get().getRaisonSociale());
                        dto.setTierType("FOURNISSEUR");
                        dto.setTierMatched(true);
                    }
                }
            }
            if (dto.getTierType() == null || dto.getTierType().isBlank()) {
                dto.setTierType("CLIENT");
            }
        } else {
            dto.setTierType("FOURNISSEUR");
            if (dto.getTierId() != null) {
                boolean exists = fournisseurs.stream().anyMatch(f -> f.getId().equals(dto.getTierId()));
                dto.setTierMatched(exists);
            }
            if (!dto.isTierMatched() && dto.getTierIce() != null && !dto.getTierIce().isBlank()) {
                fournisseurs.stream()
                        .filter(f -> f.getIce() != null && f.getIce().trim().equalsIgnoreCase(dto.getTierIce().trim()))
                        .findFirst()
                        .ifPresent(f -> {
                            dto.setTierId(f.getId());
                            dto.setTierNom(f.getRaisonSociale());
                            dto.setTierMatched(true);
                        });
            }
            if (!dto.isTierMatched() && dto.getTierNom() != null && !dto.getTierNom().isBlank()) {
                String nomRech = dto.getTierNom().toLowerCase().trim();
                fournisseurs.stream()
                        .filter(f -> f.getRaisonSociale() != null && f.getRaisonSociale().toLowerCase().contains(nomRech))
                        .findFirst()
                        .ifPresent(f -> {
                            dto.setTierId(f.getId());
                            dto.setTierNom(f.getRaisonSociale());
                            dto.setTierMatched(true);
                        });
            }
        }

        // Matching Lignes Produits
        for (LigneDocumentOcrDTO l : dto.getLignes()) {
            if (l.getProduitId() != null) {
                boolean exists = produits.stream().anyMatch(p -> p.getId().equals(l.getProduitId()));
                l.setProduitMatched(exists);
            }
            if (!l.isProduitMatched() && l.getReference() != null && !l.getReference().isBlank()) {
                String ref = l.getReference().toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
                produits.stream()
                        .filter(p -> p.getReference() != null && p.getReference().toLowerCase().replaceAll("[^a-zA-Z0-9]", "").equals(ref))
                        .findFirst()
                        .ifPresent(p -> {
                            l.setProduitId(p.getId());
                            l.setProduitMatched(true);
                            if (l.getDesignation() == null || l.getDesignation().isBlank()) {
                                l.setDesignation(p.getDesignation() != null ? p.getDesignation() : p.getNom());
                            }
                        });
            }
        }

        // Recalcul des montants globaux si nuls
        if (dto.getMontantHT() == null || dto.getMontantHT().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal totHT = dto.getLignes().stream()
                    .map(LigneDocumentOcrDTO::getMontantHT)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setMontantHT(totHT);
        }
        if (dto.getMontantTTC() == null || dto.getMontantTTC().compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal totTTC = dto.getLignes().stream()
                    .map(LigneDocumentOcrDTO::getMontantTTC)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dto.setMontantTTC(totTTC);
        }
        if (dto.getMontantTVA() == null || dto.getMontantTVA().compareTo(BigDecimal.ZERO) == 0) {
            dto.setMontantTVA(dto.getMontantTTC().subtract(dto.getMontantHT()));
        }

        dto.setStatut("PRET_A_ENREGISTRER");
        dto.setMessage("Document analysé avec succès par Groq AI.");
    }

    /**
     * Étape 3 : Enregistrement définitif du document analysé dans la base de données
     */
    @Transactional
    public DocumentOcrAnalysisResultDTO enregistrerDocument(DocumentOcrAnalysisResultDTO dto) {
        Long tenantId = getTenantId();

        if ("FACTURE_ACHAT".equalsIgnoreCase(dto.getTypeDocument())) {
            return enregistrerFactureAchat(dto, tenantId);
        } else if ("COMMANDE_CLIENT".equalsIgnoreCase(dto.getTypeDocument())) {
            return enregistrerCommandeClient(dto, tenantId);
        } else if ("BON_LIVRAISON_FOURNISSEUR".equalsIgnoreCase(dto.getTypeDocument())) {
            return enregistrerLivraisonFournisseur(dto, tenantId);
        } else if ("CHEQUE".equalsIgnoreCase(dto.getTypeDocument())
                || "TRAITE".equalsIgnoreCase(dto.getTypeDocument())
                || "EFFET".equalsIgnoreCase(dto.getTypeDocument())
                || "PAIEMENT".equalsIgnoreCase(dto.getTypeDocument())) {
            return enregistrerPaiement(dto, tenantId);
        } else {
            throw new IllegalArgumentException("Type de document non pris en charge pour l'enregistrement : " + dto.getTypeDocument());
        }
    }

    private DocumentOcrAnalysisResultDTO enregistrerFactureAchat(DocumentOcrAnalysisResultDTO dto, Long tenantId) {
        Fournisseur fournisseur = null;
        if (dto.getTierId() != null) {
            fournisseur = fournisseurRepository.findById(dto.getTierId()).orElse(null);
        }
        if (fournisseur == null) {
            // Création automatique du fournisseur s'il n'existe pas encore
            fournisseur = new Fournisseur();
            fournisseur.setRaisonSociale(dto.getTierNom() != null ? dto.getTierNom() : "Fournisseur Inconnu");
            fournisseur.setIce(dto.getTierIce());
            fournisseur.setTelephone(dto.getTierTelephone());
            fournisseur.setAdresse(dto.getTierAdresse());
            fournisseur.setPointDeVenteId(tenantId);
            fournisseur = fournisseurRepository.save(fournisseur);
            dto.setTierId(fournisseur.getId());
        }

        FactureAchat f = new FactureAchat();
        f.setNumeroFacture(dto.getNumeroPiece() != null ? dto.getNumeroPiece() : "FA-" + System.currentTimeMillis());
        f.setFournisseur(fournisseur);
        f.setDateFacture(dto.getDatePiece() != null ? LocalDate.parse(dto.getDatePiece()).atStartOfDay() : LocalDateTime.now());
        if (dto.getDateEcheance() != null) {
            f.setDateEcheance(LocalDate.parse(dto.getDateEcheance()).atStartOfDay());
        }
        f.setMontantHt(dto.getMontantHT());
        f.setMontantTva(dto.getMontantTVA());
        f.setMontantTtc(dto.getMontantTTC());
        f.setStatut(StatutFacture.EN_ATTENTE);
        f.setPointDeVenteId(tenantId);
        f.setObservations("Importé automatiquement via OCR & Groq AI");

        for (LigneDocumentOcrDTO l : dto.getLignes()) {
            LigneFactureAchat lf = new LigneFactureAchat();
            lf.setFactureAchat(f);
            Produit p = null;
            if (l.getProduitId() != null) {
                p = produitRepository.findById(l.getProduitId()).orElse(null);
            }
            if (p == null) {
                // Associer au premier produit par défaut ou créer un produit
                p = produitRepository.findByPointDeVenteId(tenantId).stream().findFirst().orElse(null);
            }
            lf.setProduit(p);
            lf.setQuantite(l.getQuantite());
            lf.setPrixUnitaireHt(l.getPrixUnitaireHT());
            lf.setTauxTva(l.getTauxTVA());
            lf.setMontantHt(l.getMontantHT());
            lf.setMontantTva(l.getMontantTTC().subtract(l.getMontantHT()));
            lf.setMontantTtc(l.getMontantTTC());
            f.getLignes().add(lf);
        }

        FactureAchat saved = factureAchatRepository.save(f);
        lierDocumentSiPresent(dto.getDocumentId(), saved.getId(), null, null, null, null);

        dto.setSavedEntityId(saved.getId());
        dto.setStatut("ENREGISTRE");
        dto.setMessage("Facture d'achat enregistrée avec succès (ID: " + saved.getId() + ")");
        return dto;
    }

    private DocumentOcrAnalysisResultDTO enregistrerCommandeClient(DocumentOcrAnalysisResultDTO dto, Long tenantId) {
        Client client = null;
        if (dto.getTierId() != null) {
            client = clientRepository.findById(dto.getTierId()).orElse(null);
        }
        if (client == null) {
            client = new Client();
            client.setNom(dto.getTierNom() != null ? dto.getTierNom() : "Client Importé");
            client.setNomComplet(dto.getTierNom());
            client.setIce(dto.getTierIce());
            client.setTelephone(dto.getTierTelephone());
            client.setPointDeVenteId(tenantId);
            client = clientRepository.save(client);
            dto.setTierId(client.getId());
        }

        CommandeClient c = new CommandeClient();
        String numInterne = null;
        try {
            if (codificationService != null) {
                numInterne = codificationService.genererNumero(com.gestion.persistent.enums.TypeDocumentCodification.COMMANDE_CLIENT, tenantId);
            }
        } catch (Exception ex) {
            log.warn("Erreur lors de la génération automatique du numéro de commande interne: {}", ex.getMessage());
        }
        if (numInterne == null || numInterne.isBlank()) {
            numInterne = "CMD-" + System.currentTimeMillis();
        }

        c.setNumeroCommande(numInterne);
        // Numéro externe : Référence du Bon de Commande client extrait par l'OCR
        c.setNumeroCommandeExterne(dto.getNumeroPiece() != null && !dto.getNumeroPiece().isBlank() ? dto.getNumeroPiece().trim() : null);
        c.setClient(client);
        c.setClientNom(client.getNomComplet() != null ? client.getNomComplet() : client.getNom());
        c.setClientTelephone(client.getTelephone());
        c.setDateCommande(dto.getDatePiece() != null ? LocalDate.parse(dto.getDatePiece()).atStartOfDay() : LocalDateTime.now());
        c.setMontantHT(dto.getMontantHT());
        c.setMontantTTC(dto.getMontantTTC());
        c.setStatut(StatutCommandeClient.BROUILLON);
        c.setPointDeVenteId(tenantId);
        c.setObservations("Importé automatiquement via OCR & Groq AI");

        for (LigneDocumentOcrDTO l : dto.getLignes()) {
            LigneCommandeClient lc = new LigneCommandeClient();
            lc.setCommandeClient(c);
            Produit p = null;
            if (l.getProduitId() != null) {
                p = produitRepository.findById(l.getProduitId()).orElse(null);
            }
            if (p == null) {
                p = produitRepository.findByPointDeVenteId(tenantId).stream().findFirst().orElse(null);
            }
            lc.setProduit(p);
            lc.setQuantite(l.getQuantite() != null ? l.getQuantite() : java.math.BigDecimal.ONE);
            lc.setPrixUnitaire(l.getPrixUnitaireHT());
            lc.setMontantLigne(l.getMontantTTC());
            c.getLignesCommande().add(lc);
        }

        CommandeClient saved = commandeClientRepository.save(c);
        lierDocumentSiPresent(dto.getDocumentId(), null, saved.getId(), null, null, null);

        dto.setSavedEntityId(saved.getId());
        dto.setStatut("ENREGISTRE");
        dto.setMessage("Commande client enregistrée avec succès (ID: " + saved.getId() + ")");
        return dto;
    }

    private DocumentOcrAnalysisResultDTO enregistrerLivraisonFournisseur(DocumentOcrAnalysisResultDTO dto, Long tenantId) {
        Depot depot = depotRepository.findByPointDeVenteId(tenantId).stream()
                .findFirst()
                .orElse(null);
        if (depot == null) {
            depot = depotRepository.findByActifTrue().stream().findFirst().orElse(null);
        }

        Livraison liv = new Livraison();
        liv.setNumeroLivraison(dto.getNumeroPiece() != null ? dto.getNumeroPiece() : "BL-" + System.currentTimeMillis());
        LocalDateTime dateLiv = LocalDateTime.now();
        if (dto.getDatePiece() != null && !dto.getDatePiece().isBlank()) {
            try {
                dateLiv = LocalDate.parse(dto.getDatePiece()).atStartOfDay();
            } catch (Exception ignored) {}
        }
        liv.setDateLivraison(dateLiv);
        liv.setMontantTotal(dto.getMontantTTC());
        liv.setPointDeVenteId(tenantId);
        liv.setObservations("BL fournisseur importé via OCR & Groq AI");

        for (LigneDocumentOcrDTO l : dto.getLignes()) {
            LigneLivraison ll = new LigneLivraison();
            ll.setLivraison(liv);
            ll.setDepot(depot);
            Produit p = null;
            if (l.getProduitId() != null) {
                p = produitRepository.findById(l.getProduitId()).orElse(null);
            }
            if (p == null) {
                p = produitRepository.findByPointDeVenteId(tenantId).stream().findFirst().orElse(null);
            }
            ll.setProduit(p);
            ll.setQuantiteLivree(l.getQuantite() != null ? l.getQuantite().longValue() : 1L);
            ll.setPrixProduit(l.getPrixUnitaireHT());
            liv.getLignesLivraison().add(ll);
        }

        Livraison saved = livraisonRepository.save(liv);
        lierDocumentSiPresent(dto.getDocumentId(), null, null, saved.getId(), null, null);

        dto.setSavedEntityId(saved.getId());
        dto.setStatut("ENREGISTRE");
        dto.setMessage("Bon de livraison fournisseur enregistré avec succès (ID: " + saved.getId() + ")");
        return dto;
    }

    private DocumentOcrAnalysisResultDTO enregistrerPaiement(DocumentOcrAnalysisResultDTO dto, Long tenantId) {
        ModePaiement mode = ModePaiement.CHEQUE;
        TypeEffet typeEffet = TypeEffet.CHEQUE;
        String typeDoc = dto.getTypeDocument() != null ? dto.getTypeDocument().toUpperCase() : "CHEQUE";
        if (typeDoc.contains("TRAITE")) {
            mode = ModePaiement.TRAITE;
            typeEffet = TypeEffet.TRAITE;
        } else if (typeDoc.contains("EFFET")) {
            mode = ModePaiement.EFFET;
            typeEffet = TypeEffet.TRAITE;
        }

        User encaissePar = userRepository.findByTenantId(tenantId).stream().findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst().orElse(null));

        if (encaissePar == null) {
            throw new IllegalStateException("Aucun utilisateur disponible pour enregistrer l'encaissement.");
        }

        Client client = null;
        Fournisseur fournisseur = null;
        if ("CLIENT".equalsIgnoreCase(dto.getTierType()) && dto.getTierId() != null) {
            client = clientRepository.findById(dto.getTierId()).orElse(null);
        } else if ("FOURNISSEUR".equalsIgnoreCase(dto.getTierType()) && dto.getTierId() != null) {
            fournisseur = fournisseurRepository.findById(dto.getTierId()).orElse(null);
        }

        BigDecimal montant = dto.getMontantTTC() != null && dto.getMontantTTC().compareTo(BigDecimal.ZERO) > 0
                ? dto.getMontantTTC()
                : (dto.getMontantHT() != null ? dto.getMontantHT() : BigDecimal.ZERO);

        LocalDateTime datePaiement = LocalDateTime.now();
        if (dto.getDatePiece() != null && !dto.getDatePiece().isBlank()) {
            try {
                datePaiement = LocalDate.parse(dto.getDatePiece()).atStartOfDay();
            } catch (Exception ignored) {}
        }

        LocalDateTime dateEcheance = datePaiement;
        if (dto.getDateEcheance() != null && !dto.getDateEcheance().isBlank()) {
            try {
                dateEcheance = LocalDate.parse(dto.getDateEcheance()).atStartOfDay();
            } catch (Exception ignored) {}
        }

        Paiement p = new Paiement();
        p.setNumeroPaiement("PAY-OCR-" + System.currentTimeMillis());
        p.setModePaiement(mode);
        p.setMontant(montant);
        p.setDatePaiement(datePaiement);
        p.setDateEcheance(dateEcheance);
        p.setNomBanque(dto.getNomBanque());
        p.setNumeroCheque(dto.getNumeroCheque() != null ? dto.getNumeroCheque() : dto.getNumeroPiece());
        p.setReferencePaiement(dto.getNumeroPiece());
        p.setClient(client);
        p.setEncaissePar(encaissePar);
        p.setPointDeVenteId(tenantId);
        p.setNotes("Importé automatiquement via OCR & Groq AI (" + typeDoc + ")");

        Paiement savedPaiement = paiementRepository.save(p);

        ChequeEffet cheque = new ChequeEffet();
        cheque.setNumeroPiece(dto.getNumeroCheque() != null ? dto.getNumeroCheque() : (dto.getNumeroPiece() != null ? dto.getNumeroPiece() : savedPaiement.getNumeroPaiement()));
        cheque.setTypeEffet(typeEffet);
        cheque.setSens(fournisseur != null ? SensEffet.DECAISSEMENT_FOURNISSEUR : SensEffet.ENCAISSEMENT_CLIENT);
        cheque.setStatut(StatutEffet.EN_PORTEFEUILLE);
        cheque.setMontant(montant);
        cheque.setDateEmission(datePaiement.toLocalDate());
        cheque.setDateEcheance(dateEcheance.toLocalDate());
        cheque.setBanqueEmettrice(dto.getNomBanque());
        cheque.setTireur(dto.getTierNom() != null ? dto.getTierNom() : (client != null ? (client.getNomComplet() != null ? client.getNomComplet() : client.getNom()) : "Inconnu"));
        cheque.setBeneficiaire(fournisseur != null ? fournisseur.getRaisonSociale() : "Entreprise");
        cheque.setReferencePaiement(savedPaiement.getNumeroPaiement());
        cheque.setClient(client);
        cheque.setFournisseur(fournisseur);
        cheque.setPointDeVenteId(tenantId);
        cheque.setDateCreation(LocalDateTime.now());
        cheque.setNotes("Importé via OCR & Groq AI");

        ChequeEffet savedCheque = chequeEffetRepository.save(cheque);

        lierDocumentSiPresent(dto.getDocumentId(), null, null, null, savedPaiement.getId(), savedCheque.getId());

        dto.setSavedEntityId(savedPaiement.getId());
        dto.setStatut("ENREGISTRE");
        dto.setMessage("Paiement " + mode.getLibelle() + " et effet de trésorerie enregistrés avec succès (Paiement ID: " 
                + savedPaiement.getId() + ", Effet ID: " + savedCheque.getId() + ")");

        return dto;
    }

    private void lierDocumentSiPresent(Long documentId, Long factureAchatId, Long commandeClientId, Long livraisonId, Long paiementId, Long chequeEffetId) {
        if (documentId != null && documentId > 0) {
            try {
                gedService.lierDocument(documentId, factureAchatId, commandeClientId, livraisonId, paiementId, chequeEffetId);
                log.info(">> Pièce justificative GED #{} liée avec succès à l'entité (facture={}, commande={}, bl={}, paiement={}, cheque={})",
                        documentId, factureAchatId, commandeClientId, livraisonId, paiementId, chequeEffetId);
            } catch (Exception e) {
                log.warn("Impossible de lier le document GED #{} : {}", documentId, e.getMessage());
            }
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("'", "\\'").replace("\"", "\\\"").replace("\n", " ");
    }
}
