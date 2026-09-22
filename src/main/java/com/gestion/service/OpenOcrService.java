package com.gestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


@Service
public class OpenOcrService {

    private static final Logger log = LoggerFactory.getLogger(OpenOcrService.class);

    @Value("${openocr.api.url:https://api.open-ocr.com/v1}")
    private String apiUrl;

    @Value("${openocr.api.key:sk-ocr-421a8f647b2a47f190c5c2d2b6fd0d5d}")
    private String apiKey;

    @Value("${openocr.engine:openrouter/qwen/qwen3-vl-235b-a22b-instruct}")
    private String defaultEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Extrait le texte d'un fichier (PDF, JPG, PNG) via OpenOCR.
     */
    public String extraireTexte(byte[] fileBytes, String fileName, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        String mimeType = determinerMimeType(fileName, contentType);
        String base64Data = Base64.getEncoder().encodeToString(fileBytes);
        String engine = (defaultEngine != null && !defaultEngine.isBlank()) 
                ? defaultEngine.trim() 
                : "openrouter/qwen/qwen3-vl-235b-a22b-instruct";

        try {
            Map<String, Object> inputObj = new HashMap<>();
            inputObj.put("type", "base64");
            inputObj.put("data_base64", base64Data);
            inputObj.put("mime_type", mimeType);

            Map<String, Object> settingsObj = new HashMap<>();
            settingsObj.put("language", "fra");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("engine", engine);
            requestBody.put("input", inputObj);
            requestBody.put("settings", settingsObj);

            String jsonPayload = objectMapper.writeValueAsString(requestBody);

            String endpoint = apiUrl.endsWith("/") ? apiUrl + "ocr" : apiUrl + "/ocr";
            String idempotencyKey = "req-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Appel OpenOCR ({}) sur {} pour fichier: {} ({}, {} octets, idempotency={})", 
                    engine, endpoint, fileName, mimeType, fileBytes.length, idempotencyKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Idempotency-Key", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            log.info("==================== [OPENOCR REPONSE BRUTE HTTP {}] ====================\n{}\n=========================================================================", 
                    response.statusCode(), responseBody);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String extracted = extraireContenuTexte(responseBody);
                log.info("OpenOCR succès [{}] pour {} : {} caractères extraits", engine, fileName, extracted != null ? extracted.length() : 0);
                return extracted != null ? extracted : responseBody;
            } else {
                log.error("Erreur OpenOCR (code {}): {}", response.statusCode(), responseBody);
                return fallbackExtraction(fileBytes, fileName, mimeType, "Erreur API OpenOCR (" + response.statusCode() + ")");
            }
        } catch (Exception e) {
            log.warn("Échec de la connexion à OpenOCR: {}. Basculement sur fallback local.", e.getMessage());
            return fallbackExtraction(fileBytes, fileName, mimeType, e.getMessage());
        }
    }

    /**
     * Analyse flexible de la réponse JSON retournée par OpenOCR selon le modèle utilisé (OCR classique ou LLM Vision)
     */
    private String extraireContenuTexte(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.hasNonNull("extracted_text")) {
                return root.get("extracted_text").asText();
            }
            if (root.hasNonNull("text")) {
                return root.get("text").asText();
            }
            if (root.hasNonNull("content")) {
                return root.get("content").asText();
            }
            if (root.has("choices") && root.get("choices").isArray() && !root.get("choices").isEmpty()) {
                JsonNode firstChoice = root.get("choices").get(0);
                if (firstChoice.has("message") && firstChoice.get("message").hasNonNull("content")) {
                    return firstChoice.get("message").get("content").asText();
                } else if (firstChoice.hasNonNull("text")) {
                    return firstChoice.get("text").asText();
                }
            }
            if (root.has("data")) {
                JsonNode dataNode = root.get("data");
                if (dataNode.hasNonNull("extracted_text")) return dataNode.get("extracted_text").asText();
                if (dataNode.hasNonNull("text")) return dataNode.get("text").asText();
                if (dataNode.hasNonNull("content")) return dataNode.get("content").asText();
            }
            if (root.hasNonNull("result")) {
                return root.get("result").isTextual() ? root.get("result").asText() : root.get("result").toString();
            }
        } catch (Exception e) {
            log.warn("Erreur parsing JSON réponse OpenOCR: {}", e.getMessage());
        }
        return json;
    }

    private String determinerMimeType(String fileName, String contentType) {
        if (contentType != null && !contentType.isBlank() && !contentType.equals("application/octet-stream")) {
            return contentType;
        }
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".pdf")) return "application/pdf";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".webp")) return "image/webp";
            if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        }
        return "application/pdf";
    }

    /**
     * Fallback de secours si OpenOCR n'est pas joignable (ex: tests hors ligne)
     */
    private String fallbackExtraction(byte[] fileBytes, String fileName, String mimeType, String errorReason) {
        if ("application/pdf".equals(mimeType)) {
            String raw = new String(fileBytes, StandardCharsets.ISO_8859_1);
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            while ((idx = raw.indexOf("BT", idx)) != -1) {
                int endIdx = raw.indexOf("ET", idx);
                if (endIdx > idx) {
                    String chunk = raw.substring(idx, endIdx);
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\(([^)]+)\\)").matcher(chunk);
                    while (m.find()) {
                        sb.append(m.group(1)).append(" ");
                    }
                    sb.append("\n");
                    idx = endIdx + 2;
                } else {
                    break;
                }
            }
            if (sb.length() > 50) {
                log.info("Fallback PDF texte extrait {} caractères", sb.length());
                return sb.toString();
            }
        }

        throw new RuntimeException("Impossible d'extraire le texte du relevé bancaire via OpenOCR (" + errorReason + "). Veuillez vérifier la connexion ou le document.");
    }
}
