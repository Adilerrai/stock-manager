package com.gestion.service.pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class BanquePatternRegistry {

    private static final Logger log = LoggerFactory.getLogger(BanquePatternRegistry.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BanquePatternConfig> patterns = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        chargerPatterns();
    }

    public synchronized void chargerPatterns() {
        patterns.clear();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:banques-patterns/*.json");
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream()) {
                    BanquePatternConfig config = objectMapper.readValue(is, BanquePatternConfig.class);
                    if (config.getCode() != null) {
                        patterns.put(config.getCode().toUpperCase(), config);
                        log.info("Modèle bancaire JSON chargé : {} ({})", config.getNom(), config.getCode());
                    }
                } catch (Exception e) {
                    log.error("Erreur lors du chargement du pattern JSON {}: {}", res.getFilename(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Impossible de scanner les fichiers de patterns JSON: {}", e.getMessage());
        }

        // Si aucun fichier n'a été trouvé dans le classpath, initialiser le modèle CIH par défaut
        if (patterns.isEmpty()) {
            initCihParDefaut();
        }
    }

    private void initCihParDefaut() {
        BanquePatternConfig cih = new BanquePatternConfig();
        cih.setCode("CIH");
        cih.setNom("CIH Bank");
        cih.setDetectionRegex("(?i)(CIH\\s*BANK|\\bCIH\\b|230\\s*780|CIHMOBILEBILLING)");
        cih.setAnneeRegex("\\b(202[0-9])\\b");
        cih.setLigneTransactionRegex("^(?<dateOp>\\d{2}[/.-]\\d{2})\\s+(?<dateVal>\\d{2}[/.-]\\d{2})\\s+(?<libelle>.+?)\\s+(?<montant>[+-]?\\s*\\d{1,3}(?:[\\s. ]\\d{3})*(?:[.,]\\d{2})|[+-]?\\s*\\d+(?:[.,]\\d{2}))$");
        cih.setColonnes(List.of("dateOperation", "dateValeur", "libelle", "montant"));

        BanquePatternConfig.ReglesSens regles = new BanquePatternConfig.ReglesSens();
        regles.setPriorite("CREDIT_FIRST");
        regles.setMotsClesCredit(List.of("RECU", "REÇU", "VIREMENT RECU", "VIREMENT INSTANTANE RECU", "VERSEMENT", "REMISE", "ENCAISSEMENT", "CREDIT"));
        regles.setMotsClesDebit(List.of("VIREMENT EMIS", "PAIEMENT", "FACTURE", "RETRAIT", "COMMISSION", "COTISATION", "PRELEVEMENT", "RECHARGE", "ACHAT"));
        cih.setReglesSens(regles);

        cih.setLignesIgnorees(List.of("SOLDE DEPART", "SOLDE PRECEDENT", "ANCIEN SOLDE", "NOUVEAU SOLDE", "PAGE N", "RELEVE DE COMPTE", "DATES OPERATION"));
        patterns.put("CIH", cih);
        log.info("Modèle bancaire CIH chargé en secours");
    }

    /**
     * Détecte automatiquement le modèle de banque correspondant au texte brut OCR
     */
    public BanquePatternConfig trouverPattern(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return null;
        }

        for (BanquePatternConfig config : patterns.values()) {
            if (config.getDetectionRegex() != null) {
                try {
                    Pattern p = Pattern.compile(config.getDetectionRegex(), Pattern.CASE_INSENSITIVE);
                    if (p.matcher(rawText).find()) {
                        log.info("Banque identifiée par pattern : {} ({})", config.getNom(), config.getCode());
                        return config;
                    }
                } catch (Exception e) {
                    log.warn("Regex invalide pour pattern {}: {}", config.getCode(), e.getMessage());
                }
            }
            if (config.getIdentifiants() != null) {
                String upper = rawText.toUpperCase();
                for (String id : config.getIdentifiants()) {
                    if (upper.contains(id.toUpperCase())) {
                        log.info("Banque identifiée par mot-clé : {} ({})", config.getNom(), config.getCode());
                        return config;
                    }
                }
            }
        }
        return null;
    }

    public BanquePatternConfig getPattern(String code) {
        return code != null ? patterns.get(code.toUpperCase()) : null;
    }

    public List<BanquePatternConfig> getAllPatterns() {
        return new ArrayList<>(patterns.values());
    }
}
