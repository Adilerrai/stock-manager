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


    // Regex pour détecter les dates
    private static final Pattern PATTERN_DATE = Pattern.compile("(?<!\\d)(\\d{2}[/.-]\\d{2}[/.-]\\d{2,4})(?!\\d)");
    
    // Regex pour détecter les montants (ex: 25 000.00, 25.000,00, 25000,00, -8 000,50, +150.00)
    private static final Pattern PATTERN_MONTANT = Pattern.compile("(?<!\\w)([+-]?\\s*\\d{1,3}(?:[\\s. ]\\d{3})*(?:[.,]\\d{2})|[+-]?\\s*\\d+(?:[.,]\\d{2}))(?![\\w%])");

    // Mots-clés typiques de sorties de fonds (Débit banque)
    private static final Set<String> MOTS_CLES_DEBIT = Set.of(
            "FRAIS", "COMMISSION", "COTISATION", "RETRAIT", "PAIEMENT", "PRELEVEMENT", "PRLV",
            "CHQ", "CHEQUE", "VIREMENT EMIS", "VIR FOURNISSEUR", "AGIOS", "TAXE", "CARTE", "DAB", "GAB", "DEBIT"
    );

    // Mots-clés typiques d'entrées de fonds (Crédit banque)
    private static final Set<String> MOTS_CLES_CREDIT = Set.of(
            "VIREMENT RECU", "VIR RECU", "VIR CLIENT", "REMISE", "ENCAISSEMENT", "VERSEMENT",
            "AVOIR", "CREDIT", "DEPOT", "INTERETS CREDITEURS"
    );

    /**
     * Analyse le texte brut OCR et extrait les lignes de transactions bancaires normalisées.
     */
    public List<LigneReleveBancaireDTO> extraireTransactions(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return Collections.emptyList();
        }

        List<LigneReleveBancaireDTO> transactions = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");

        LocalDate derniereDateTrouvee = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Ignorer les lignes d'en-tête génériques ou de pieds de page
            if (isHeaderOrFooter(trimmed)) continue;

            Matcher dateMatcher = PATTERN_DATE.matcher(trimmed);
            List<String> dates = new ArrayList<>();
            while (dateMatcher.find()) {
                dates.add(dateMatcher.group(1));
            }

            // Si aucune date sur la ligne mais qu'on a déjà vu une date et qu'il y a un montant
            LocalDate dateOp = null;
            LocalDate dateVal = null;

            if (!dates.isEmpty()) {
                dateOp = parseDate(dates.get(0));
                if (dates.size() > 1) {
                    dateVal = parseDate(dates.get(1));
                } else {
                    dateVal = dateOp;
                }
                if (dateOp != null) {
                    derniereDateTrouvee = dateOp;
                }
            } else if (derniereDateTrouvee != null) {
                // Peut être une ligne de suite
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
                // Si deux montants distincts sont détectés : souvent Colonne Débit puis Colonne Crédit
                BigDecimal premier = montants.get(montants.size() - 2);
                BigDecimal second = montants.get(montants.size() - 1);
                // Si l'un est zéro ou si format classique
                if (isProbableDebit(libelle)) {
                    debit = premier.abs();
                } else {
                    credit = second.abs();
                }
            } else {
                BigDecimal uniqueMontant = montants.get(montants.size() - 1);
                String raw = rawMontants.get(rawMontants.size() - 1);

                if (raw.contains("-") || uniqueMontant.compareTo(BigDecimal.ZERO) < 0) {
                    debit = uniqueMontant.abs();
                } else if (raw.contains("+")) {
                    credit = uniqueMontant.abs();
                } else if (isProbableDebit(libelle)) {
                    debit = uniqueMontant.abs();
                } else if (isProbableCredit(libelle)) {
                    credit = uniqueMontant.abs();
                } else {
                    // Par défaut si pas d'autre indice, selon le sens le plus commun ou libellé
                    debit = uniqueMontant.abs();
                }
            }

            if (libelle.length() > 490) {
                libelle = libelle.substring(0, 490);
            }

            // Référence extraite (ex: VIR-12345, CHQ 987654)
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

    private boolean isProbableDebit(String text) {
        String upper = text.toUpperCase();
        for (String mot : MOTS_CLES_DEBIT) {
            if (upper.contains(mot)) return true;
        }
        return false;
    }

    private boolean isProbableCredit(String text) {
        String upper = text.toUpperCase();
        for (String mot : MOTS_CLES_CREDIT) {
            if (upper.contains(mot)) return true;
        }
        return false;
    }

    private String extraireReference(String libelle) {
        Pattern refPattern = Pattern.compile("(?i)(?:REF|VIR|CHQ|FACTURE|PIECE|N°|AVIS)[:\\s]*([A-Z0-9_-]{4,20})");
        Matcher m = refPattern.matcher(libelle);
        if (m.find()) {
            return m.group(1).trim();
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
               (l.contains("DATE") && l.contains("LIBELLE") && l.contains("DEBIT"));
    }

    private LocalDate parseDate(String val) {
        if (val == null) return null;
        val = val.trim().replace(".", "/").replace("-", "/");
        String[] parts = val.split("/");
        if (parts.length != 3) return null;

        try {
            int jour = Integer.parseInt(parts[0]);
            int mois = Integer.parseInt(parts[1]);
            int annee = Integer.parseInt(parts[2]);
            if (annee < 100) annee += 2000;
            return LocalDate.of(annee, mois, jour);
        } catch (Exception e) {
            return null;
        }
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
