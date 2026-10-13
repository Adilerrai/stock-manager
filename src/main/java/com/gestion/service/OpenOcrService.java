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

    @Value("${doctr.api.url:${DOCTR_API_URL:http://5.189.162.46:8002}}")
    private String doctrApiUrl;

    @Value("${openocr.api.url:https://api.open-ocr.com/v1}")
    private String apiUrl;

    @Value("${openocr.api.key:sk-ocr-421a8f647b2a47f190c5c2d2b6fd0d5d}")
    private String apiKey;

    @Value("${openocr.engine:openrouter/qwen/qwen3-vl-235b-a22b-instruct}")
    private String defaultEngine;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /**
     * Extrait le texte d'un fichier (PDF, JPG, PNG) en priorité absolue via docTR OCR,
     * avec basculement automatique sur OpenOCR en cas d'indisponibilité.
     */
    public String extraireTexte(byte[] fileBytes, String fileName, String contentType) {
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalArgumentException("Le fichier est vide.");
        }

        String mimeType = determinerMimeType(fileName, contentType);

        // 1. Appel du microservice docTR OCR (100% local, privé)
        String doctrText = extraireTexteViaDocTr(fileBytes, fileName, mimeType);
        if (doctrText != null && !doctrText.isBlank()) {
            log.info("docTR OCR utilisé avec succès pour {} (longueur texte : {})", fileName, doctrText.length());
            return doctrText;
        }

        log.warn("docTR OCR n'a pas retourné de texte pour {}. Tentative d'extraction texte locale (si PDF).", fileName);
        return fallbackExtraction(fileBytes, fileName, mimeType, "docTR OCR non disponible");
    }

    /**
     * Appel multipart/form-data vers le microservice docTR OCR (FastAPI).
     */
    private String extraireTexteViaDocTr(byte[] fileBytes, String fileName, String mimeType) {
        if (doctrApiUrl == null || doctrApiUrl.isBlank()) {
            return null;
        }

        try {
            String baseUrl = doctrApiUrl.trim();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String endpoint = baseUrl + "/ocr";

            log.info(">> Appel docTR OCR sur {} pour le fichier: {} (taille: {} octets)", 
                    endpoint, fileName, fileBytes.length);

            String boundary = "DocTrBoundary" + UUID.randomUUID().toString().replace("-", "");
            String safeFileName = (fileName != null && !fileName.isBlank()) 
                    ? fileName.replaceAll("[^a-zA-Z0-9._-]", "_") 
                    : "document.pdf";

            byte[] header = ("--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeFileName + "\"\r\n" +
                    "Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);

            byte[] payload = new byte[header.length + fileBytes.length + footer.length];
            System.arraycopy(header, 0, payload, 0, header.length);
            System.arraycopy(fileBytes, 0, payload, header.length, fileBytes.length);
            System.arraycopy(footer, 0, payload, header.length + fileBytes.length, footer.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.hasNonNull("text")) {
                    String text = root.get("text").asText();
                    long processingTime = root.has("processingTimeMs") ? root.get("processingTimeMs").asLong() : 0;
                    log.info("<< docTR OCR succès pour {} : {} caractères extraits en {} ms", 
                            safeFileName, text.length(), processingTime);
                    return text;
                }
            } else {
                log.warn("docTR OCR a retourné le code HTTP {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.warn("Échec de l'appel au service docTR OCR ({}): {}", doctrApiUrl, e.getMessage());
        }
        return null;
    }

    /**
     * Analyse flexible de la réponse JSON retournée par OpenOCR selon le modèle utilisé.
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
     * Fallback de secours si docTR et OpenOCR ne sont pas joignables.
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

        throw new RuntimeException("Impossible d'extraire le texte du document (" + errorReason + "). Veuillez vérifier la connexion ou le document.");
    }
}
