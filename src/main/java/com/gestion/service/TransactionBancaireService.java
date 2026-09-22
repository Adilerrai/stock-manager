package com.gestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gestion.persistent.dto.LigneReleveBancaireDTO;
import com.gestion.persistent.enums.StatutRapprochement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TransactionBancaireService {

    private static final Logger log = LoggerFactory.getLogger(TransactionBancaireService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());


    // Regex pour détecter les dates (format dd/MM/yyyy ou dd/MM sans année)
    private static final Pattern PATTERN_DATE = Pattern.compile("(?<!\\d)(\\d{2}[/.-]\\d{2}(?:[/.-]\\d{2,4})?)(?!\\d)");
    
    // Regex pour détecter les montants (ex: 25 000.00, 25.000,00, 25000,00, -8 000,50, +150.00)
    private static final Pattern PATTERN_MONTANT = Pattern.compile("(?<!\\w)([+-]?\\s*\\d{1,3}(?:[\\s. ]\\d{3})*(?:[.,]\\d{2})|[+-]?\\s*\\d+(?:[.,]\\d{2}))(?![\\w%])");

    // Mots-clés typiques de sorties de fonds (Débit banque)
    private static final Set<String> MOTS_CLES_DEBIT = Set.of(
            "FRAIS", "COMMISSION", "COTISATION", "RETRAIT", "PAIEMENT", "PRELEVEMENT", "PRLV",
            "CHQ", "CHEQUE", "VIREMENT EMIS", "VIR EMIS", "VIR FOURNISSEUR", "AGIOS", "TAXE",
            "CARTE", "DAB", "GAB", "DEBIT", "RECHARGE", "ACHAT", "BILLING PAY"
    );

    // Mots-clés typiques d'entrées de fonds (Crédit banque)
    private static final Set<String> MOTS_CLES_CREDIT = Set.of(
            "RECU", "REÇU", "VIREMENT RECU", "VIR RECU", "VIR CLIENT", "REMISE", "ENCAISSEMENT",
            "VERSEMENT", "AVOIR", "CREDIT", "DEPOT", "INTERETS CREDITEURS", "SALAIRE"
    );

    /**
     * Analyse le texte brut OCR et extrait les lignes de transactions bancaires normalisées.
     */
    public List<LigneReleveBancaireDTO> extraireTransactions(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        int defaultYear = detecterAnneeReleve(rawText);
        log.info("Année de référence détectée pour le relevé : {}", defaultYear);

        List<LigneReleveBancaireDTO> transactions = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");

        LocalDate derniereDateTrouvee = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Ignorer les lignes d'en-tête génériques, soldes ou pieds de page
            if (isHeaderOrFooter(trimmed)) continue;

            Matcher dateMatcher = PATTERN_DATE.matcher(trimmed);
            List<String> dates = new ArrayList<>();
            while (dateMatcher.find()) {
                dates.add(dateMatcher.group(1));
            }

            LocalDate dateOp = null;
            LocalDate dateVal = null;

            if (!dates.isEmpty()) {
                dateOp = parseDate(dates.get(0), defaultYear);
                if (dates.size() > 1) {
                    dateVal = parseDate(dates.get(1), defaultYear);
                } else {
                    dateVal = dateOp;
                }
                if (dateOp != null) {
                    derniereDateTrouvee = dateOp;
                }
            } else if (derniereDateTrouvee != null) {
                dateOp = derniereDateTrouvee;
                dateVal = derniereDateTrouvee;
            }

            if (dateOp == null) {
                continue;
            }

            // Extraire tous les montants de la ligne
            Matcher montantMatcher = PATTERN_MONTANT.matcher(trimmed);
            List<BigDecimal> montants = new ArrayList<>();
            List<String> rawMontants = new ArrayList<>();

            while (montantMatcher.find()) {
                String mStr = montantMatcher.group(1);
                // Ignorer si ça ressemble à une date ou un code postal (ex: 2026, 75001)
                if (isDateFragment(mStr, dates)) continue;
                BigDecimal m = parseMontant(mStr);
                if (m.compareTo(BigDecimal.ZERO) != 0) {
                    montants.add(m);
                    rawMontants.add(mStr);
                }
            }

            if (montants.isEmpty()) {
                continue;
            }

            // Isoler le libellé en retirant les dates et les montants de la ligne
            String libelle = trimmed;
            for (String d : dates) {
                libelle = libelle.replace(d, " ");
            }
            for (String rm : rawMontants) {
                libelle = libelle.replace(rm, " ");
            }
            libelle = libelle.replaceAll("\\s+", " ").trim();

            if (libelle.length() < 3) {
                libelle = "Opération bancaire du " + dateOp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }

            // Déterminer Débit et Crédit
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;

            if (montants.size() >= 2) {
                BigDecimal premier = montants.get(montants.size() - 2);
                BigDecimal second = montants.get(montants.size() - 1);
                if (isProbableCredit(libelle)) {
                    credit = second.abs();
                } else if (isProbableDebit(libelle)) {
                    debit = premier.abs();
                } else {
                    debit = premier.abs();
                }
            } else {
                BigDecimal uniqueMontant = montants.get(montants.size() - 1);
                String raw = rawMontants.get(rawMontants.size() - 1);

                if (raw.contains("-") || uniqueMontant.compareTo(BigDecimal.ZERO) < 0) {
                    debit = uniqueMontant.abs();
                } else if (raw.contains("+")) {
                    credit = uniqueMontant.abs();
                } else if (isProbableCredit(libelle)) {
                    credit = uniqueMontant.abs();
                } else if (isProbableDebit(libelle)) {
                    debit = uniqueMontant.abs();
                } else {
                    debit = uniqueMontant.abs();
                }
            }

            if (libelle.length() > 490) {
                libelle = libelle.substring(0, 490);
            }

            // Référence extraite (doit contenir au moins un chiffre, ex: VIR-12345, CHQ-987654)
            String reference = extraireReference(libelle);
            if (reference != null && reference.length() > 90) {
                reference = reference.substring(0, 90);
            }

            LigneReleveBancaireDTO dto = new LigneReleveBancaireDTO();
            dto.setDateOperation(dateOp);
            dto.setDateValeur(dateVal != null ? dateVal : dateOp);
            dto.setLibelle(libelle);
            dto.setReference(reference);
            dto.setDebit(debit);
            dto.setCredit(credit);
            dto.setStatut(StatutRapprochement.NON_RAPPROCHE);

            transactions.add(dto);
        }

        log.info("Extraction OCR terminée : {} transactions identifiées", transactions.size());
        try {
            String jsonTransactions = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(transactions);
            log.info("==================== [TRANSACTIONS BANCAIRES STRUCTUREES JSON] ====================\n{}\n===================================================================================", jsonTransactions);
        } catch (Exception e) {
            log.warn("Impossible de sérialiser les transactions en JSON: {}", e.getMessage());
        }
        return transactions;
    }

    private int detecterAnneeReleve(String rawText) {
        if (rawText != null) {
            // Chercher des années 4 chiffres (ex: 2024, 2025, 2026)
            Matcher m = Pattern.compile("\\b(202[0-9])\\b").matcher(rawText);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (Exception ignored) {}
            }
        }
        return LocalDate.now().getYear();
    }

    private boolean isProbableCredit(String text) {
        String upper = text.toUpperCase();
        // Si le libellé contient explicitement "RECU", "REÇU", "VERSEMENT", "REMISE", "CREDIT"
        if (upper.contains("RECU") || upper.contains("REÇU") || upper.contains("VERSEMENT") || upper.contains("AVOIR")) {
            return true;
        }
        for (String mot : MOTS_CLES_CREDIT) {
            if (upper.contains(mot)) return true;
        }
        return false;
    }

    private boolean isProbableDebit(String text) {
        String upper = text.toUpperCase();
        for (String mot : MOTS_CLES_DEBIT) {
            if (upper.contains(mot)) return true;
        }
        return false;
    }

    private String extraireReference(String libelle) {
        // Exiger que la référence commence par un mot-clé ET contienne au moins un chiffre
        Pattern refPattern = Pattern.compile("(?i)\\b(?:REF|REFERENCE|CHQ|CHEQUE|AVIS|FACTURE|PIECE|N°)[ :#-]+([A-Z0-9_-]*\\d[A-Z0-9_-]*)");
        Matcher m = refPattern.matcher(libelle);
        if (m.find()) {
            String ref = m.group(1).trim();
            if (ref.length() >= 3 && ref.length() <= 50) {
                return ref;
            }
        }
        return null;
    }

    private boolean isDateFragment(String montantStr, List<String> dates) {
        String clean = montantStr.replaceAll("[\\s. ,-]", "");
        for (String d : dates) {
            String cleanDate = d.replaceAll("[/.-]", "");
            if (cleanDate.contains(clean)) return true;
        }
        return false;
    }

    private boolean isHeaderOrFooter(String line) {
        String l = line.toUpperCase();
        return l.contains("PAGE ") || l.contains("IBAN") || l.contains("BIC") ||
               l.contains("RELEVE D'IDENTITE") || l.contains("TELEPHONE") ||
               l.contains("SOLDE PRECEDENT") || l.contains("ANCIEN SOLDE") ||
               l.contains("NOUVEAU SOLDE") || l.contains("TOTAL DES MOUVEMENTS") ||
               (l.contains("DATE") && l.contains("LIBELLE") && l.contains("DEBIT"));
    }

    private LocalDate parseDate(String val, int defaultYear) {
        if (val == null) return null;
        val = val.trim().replace(".", "/").replace("-", "/");
        String[] parts = val.split("/");

        try {
            if (parts.length == 2) {
                int jour = Integer.parseInt(parts[0]);
                int mois = Integer.parseInt(parts[1]);
                return LocalDate.of(defaultYear, mois, jour);
            } else if (parts.length == 3) {
                int jour = Integer.parseInt(parts[0]);
                int mois = Integer.parseInt(parts[1]);
                int annee = Integer.parseInt(parts[2]);
                if (annee < 100) annee += 2000;
                return LocalDate.of(annee, mois, jour);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private BigDecimal parseMontant(String val) {
        if (val == null) return BigDecimal.ZERO;
        String clean = val.replaceAll("[\\s ]", "").replace(",", ".");
        try {
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
