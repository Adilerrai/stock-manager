package com.gestion.service;

import com.gestion.persistent.dto.ImputationSuggestionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class ImputationIntelligenteService {

    public ImputationSuggestionDTO suggererImputation(String libelle, String typeOperation, BigDecimal montant) {
        if (libelle == null || libelle.trim().isEmpty()) {
            libelle = "DEPENSE COURANTE";
        }

        String texte = normaliser(libelle);
        ImputationSuggestionDTO sugg = new ImputationSuggestionDTO();

        // 1. TÉLÉCOMS, INTERNET & FRAIS POSTAUX (6145)
        if (contientUnDesMots(texte, "MAROC TELECOM", "IAM", "INWI", "ORANGE", "FIBRE", "INTERNET", "TELEPHONE", "GSM", "POSTE", "COURRIER", "CHRONOPOST", "DHL", "FEDEX", "TIMBRE")) {
            sugg.setNumeroCompteChargeProduit("61455000");
            sugg.setLibelleCompteChargeProduit("Frais de téléphone et de télécommunications");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("COMMUNICATION & TELECOM");
            sugg.setScoreConfiance(0.98);
            sugg.setExplication("Opérateur télécom ou service postal détecté dans le libellé.");
        }
        // 2. ÉNERGIE, EAU & ÉLECTRICITÉ (61251)
        else if (contientUnDesMots(texte, "ONEE", "REDAL", "LYDEC", "AMENDIS", "RADEE", "ELECTRICITE", "EAU", "SONLEEC")) {
            sugg.setNumeroCompteChargeProduit("61251000");
            sugg.setLibelleCompteChargeProduit("Achats non stockés de matières - Eau, électricité");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("14.00")); // Électricité à 14%
            sugg.setCategorieImputation("EAU & ELECTRICITE");
            sugg.setScoreConfiance(0.97);
            sugg.setExplication("Régie d'eau/électricité ou facture de fluide détectée (TVA spécifique 14%).");
        }
        // 3. CARBURANTS, PÉAGES & TRANSPORT (61253 / 6143)
        else if (contientUnDesMots(texte, "TOTAL", "SHELL", "AFRIQUIA", "WINXO", "PETROM", "OLA", "CARBURANT", "GASOIL", "ESSENCE", "DIESEL", "JAWAZ", "AUTOROUTE", "PEAGE", "TAXI", "TRAIN", "ONCF", "VOL", "RAM", "AVION")) {
            if (contientUnDesMots(texte, "JAWAZ", "AUTOROUTE", "PEAGE", "TAXI", "TRAIN", "ONCF", "VOL", "RAM", "AVION")) {
                sugg.setNumeroCompteChargeProduit("61435000");
                sugg.setLibelleCompteChargeProduit("Voyages, déplacements et réceptions");
                sugg.setTauxTvaSuggere(new BigDecimal("14.00"));
                sugg.setCategorieImputation("DEPLACEMENT & PEAGE");
            } else {
                sugg.setNumeroCompteChargeProduit("61253000");
                sugg.setLibelleCompteChargeProduit("Achats de carburants et combustibles");
                sugg.setTauxTvaSuggere(new BigDecimal("10.00")); // Carburants à 10%
                sugg.setCategorieImputation("CARBURANT");
            }
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setScoreConfiance(0.95);
            sugg.setExplication("Frais de déplacement ou station-service détectée.");
        }
        // 4. LOYERS & CHARGES LOCATIVES (6131)
        else if (contientUnDesMots(texte, "LOYER", "BAIL", "LOCATION BUREAU", "LOCAUX", "SYNDIC", "IMMOBILIER")) {
            sugg.setNumeroCompteChargeProduit("61311000");
            sugg.setLibelleCompteChargeProduit("Locations immobilières et charges locatives");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(BigDecimal.ZERO); // Les baux nus sont souvent exonérés
            sugg.setCategorieImputation("LOYERS & LOCATIONS");
            sugg.setScoreConfiance(0.96);
            sugg.setExplication("Loyer ou charges de copropriété détectés.");
        }
        // 5. HONORAIRES & CONSEIL (6136)
        else if (contientUnDesMots(texte, "HONORAIRE", "AVOCAT", "NOTAIRE", "FIDUCIAIRE", "COMPTABLE", "EXPERT", "AUDIT", "COMMISSAIRE", "CONSEIL", "CONSULTANT", "JURIDIQUE")) {
            sugg.setNumeroCompteChargeProduit("61365000");
            sugg.setLibelleCompteChargeProduit("Rémunérations d'intermédiaires et honoraires");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("HONORAIRES & EXPERTISE");
            sugg.setScoreConfiance(0.98);
            sugg.setExplication("Prestation intellectuelle ou profession libérale détectée.");
        }
        // 6. ASSURANCES (6134)
        else if (contientUnDesMots(texte, "ASSURANCE", "AXA", "WAFA", "SANLAM", "ATLANTA", "MUTUELLE", "POLICE", "MULTIRISQUE", "RESPONSABILITE CIVILE", "RC PRO")) {
            sugg.setNumeroCompteChargeProduit("61341000");
            sugg.setLibelleCompteChargeProduit("Primes d'assurances multirisques");
            sugg.setNumeroCompteTva(null);
            sugg.setLibelleCompteTva("Non assujetti TVA (Taxe sur contrats d'assurance)");
            sugg.setTauxTvaSuggere(BigDecimal.ZERO);
            sugg.setCategorieImputation("ASSURANCES");
            sugg.setScoreConfiance(0.97);
            sugg.setExplication("Compagnie ou prime d'assurance détectée (hors champ TVA).");
        }
        // 7. FRAIS BANCAIRES & COMMISSIONS (6147)
        else if (contientUnDesMots(texte, "AGIOS", "COMMISSION", "FRAIS BANCAIRE", "TENUE DE COMPTE", "VIREMENT", "CARTE BANCAIRE", "INTERET", "BMCE", "BP", "ATTIJARI", "CIH", "CREDIT DU MAROC", "SOCIETE GENERALE")) {
            sugg.setNumeroCompteChargeProduit("61472000");
            sugg.setLibelleCompteChargeProduit("Frais et commissions sur services bancaires");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("10.00")); // Frais bancaires à 10%
            sugg.setCategorieImputation("BANQUE & COMMISSIONS");
            sugg.setScoreConfiance(0.94);
            sugg.setExplication("Frais de tenue de compte ou agios bancaires détectés (TVA bancaire 10%).");
        }
        // 8. FOURNITURES DE BUREAU & INFORMATIQUE (6122 / 6133)
        else if (contientUnDesMots(texte, "PAPIER", "TONER", "ENCRE", "FOURNITURE", "CLAVIER", "SOURIS", "IMPRIMANTE", "DISQUE", "LOGICIEL", "SAAS", "LICENCE", "MICROSOFT", "GOOGLE", "AWS")) {
            sugg.setNumeroCompteChargeProduit("61227000");
            sugg.setLibelleCompteChargeProduit("Achats de fournitures de bureau et informatique");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("FOURNITURES & LOGICIELS");
            sugg.setScoreConfiance(0.93);
            sugg.setExplication("Fournitures consommables ou service informatique détecté.");
        }
        // 9. PUBLICITÉ & MARKETING (6144)
        else if (contientUnDesMots(texte, "PUBLICITE", "PUB", "MARKETING", "FACEBOOK", "META", "INSTAGRAM", "GOOGLE ADS", "FLYER", "CATALOGUE", "IMPRESSION", "SPONSORING", "SITE WEB")) {
            sugg.setNumeroCompteChargeProduit("61441000");
            sugg.setLibelleCompteChargeProduit("Publicité, publications et relations publiques");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("PUBLICITE & MARKETING");
            sugg.setScoreConfiance(0.95);
            sugg.setExplication("Campagne publicitaire ou support de communication détecté.");
        }
        // 10. VENTES (si type = VENTE)
        else if ("VENTE".equalsIgnoreCase(typeOperation) || contientUnDesMots(texte, "VENTE", "CLIENT", "COMMERCE", "MARCHANDISE")) {
            sugg.setNumeroCompteChargeProduit("71110000");
            sugg.setLibelleCompteChargeProduit("Ventes de marchandises au Maroc");
            sugg.setNumeroCompteTva("44550000");
            sugg.setLibelleCompteTva("État - TVA facturée");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("VENTES MARCHANDISES");
            sugg.setScoreConfiance(0.85);
            sugg.setExplication("Opération de vente standard suggérée.");
        }
        // 11. DÉFAUT : ACHAT MARCHANDISES OU AUTRES CHARGES EXTERNES
        else {
            sugg.setNumeroCompteChargeProduit("61110000");
            sugg.setLibelleCompteChargeProduit("Achats de marchandises (Compte général)");
            sugg.setNumeroCompteTva("34552000");
            sugg.setLibelleCompteTva("État - TVA récupérable sur charges");
            sugg.setTauxTvaSuggere(new BigDecimal("20.00"));
            sugg.setCategorieImputation("ACHAT COURANT");
            sugg.setScoreConfiance(0.70);
            sugg.setExplication("Imputation standard proposée par défaut (taux normal 20%).");
        }

        // Définition de la contrepartie par défaut
        if ("VENTE".equalsIgnoreCase(typeOperation)) {
            sugg.setNumeroCompteContrepartie("34210000");
            sugg.setLibelleCompteContrepartie("Clients");
        } else if ("CAISSE".equalsIgnoreCase(typeOperation)) {
            sugg.setNumeroCompteContrepartie("51610000");
            sugg.setLibelleCompteContrepartie("Caisses");
        } else if ("BANQUE".equalsIgnoreCase(typeOperation)) {
            sugg.setNumeroCompteContrepartie("51410000");
            sugg.setLibelleCompteContrepartie("Banques (comptes en MAD)");
        } else {
            sugg.setNumeroCompteContrepartie("44110000");
            sugg.setLibelleCompteContrepartie("Fournisseurs");
        }

        return sugg;
    }

    private String normaliser(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").toUpperCase(Locale.ROOT);
    }

    private boolean contientUnDesMots(String texte, String... mots) {
        for (String mot : mots) {
            String m = normaliser(mot);
            if (texte.contains(m)) {
                return true;
            }
        }
        return false;
    }
}
