package com.gestion.service;

import com.gestion.persistent.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ModeleEcritureService {

    private final ComptabiliteService comptabiliteService;

    // Catalogue des modèles d'écritures prédéfinis pour le marché marocain
    private static final Map<String, ModeleEcritureDTO> MODELES_STANDARDS = new LinkedHashMap<>();

    static {
        // 1. Loyer Commercial
        ModeleEcritureDTO loyer = new ModeleEcritureDTO(
                "LOYER_COMMERCIAL",
                "Loyer local commercial & bureaux",
                "Ventilation automatique du loyer HT (6131), TVA récupérable (34552) et compte fournisseur (4411)",
                "AC",
                "IMMOBILIER"
        );
        loyer.getLignes().add(new LigneModeleEcritureDTO("61310000", "Locations et charges locatives", "DEBIT", "BASE_HT", new BigDecimal("20.00")));
        loyer.getLignes().add(new LigneModeleEcritureDTO("34552000", "État - TVA récupérable sur charges", "DEBIT", "TVA_SUR_HT", new BigDecimal("20.00")));
        loyer.getLignes().add(new LigneModeleEcritureDTO("44110000", "Fournisseur bailleur", "CREDIT", "BASE_TTC", BigDecimal.ZERO));
        MODELES_STANDARDS.put(loyer.getCode(), loyer);

        // 2. Honoraires avec Retenue à la Source (RAS 10% Art. 45 bis CGI)
        ModeleEcritureDTO honoraires = new ModeleEcritureDTO(
                "HONORAIRES_AVEC_RAS",
                "Honoraires conseil/avocat/fiduciaire avec RAS 10%",
                "Ventilation Honoraires HT (6136), TVA 20% (34552), Retenue à la Source 10% (44525) et net Fournisseur (4411)",
                "AC",
                "PRESTATIONS"
        );
        honoraires.getLignes().add(new LigneModeleEcritureDTO("61360000", "Rémunérations d'intermédiaires et honoraires", "DEBIT", "BASE_HT", new BigDecimal("20.00")));
        honoraires.getLignes().add(new LigneModeleEcritureDTO("34552000", "État - TVA récupérable sur charges", "DEBIT", "TVA_SUR_HT", new BigDecimal("20.00")));
        honoraires.getLignes().add(new LigneModeleEcritureDTO("44525000", "État - Retenue à la source sur honoraires", "CREDIT", "RETENUE_SOURCE", new BigDecimal("10.00")));
        honoraires.getLignes().add(new LigneModeleEcritureDTO("44110000", "Fournisseur net à payer", "CREDIT", "SOLDE_CONTREPARTIE", BigDecimal.ZERO));
        MODELES_STANDARDS.put(honoraires.getCode(), honoraires);

        // 3. Redevance de Crédit-Bail / Leasing
        ModeleEcritureDTO leasing = new ModeleEcritureDTO(
                "LEASING_CREDIT_BAIL",
                "Redevance leasing matériel / véhicules",
                "Redevance HT (6132), TVA 20% (34552) et société de financement (4411)",
                "AC",
                "FINANCIER"
        );
        leasing.getLignes().add(new LigneModeleEcritureDTO("61320000", "Redevances de crédit-bail", "DEBIT", "BASE_HT", new BigDecimal("20.00")));
        leasing.getLignes().add(new LigneModeleEcritureDTO("34552000", "État - TVA récupérable sur charges", "DEBIT", "TVA_SUR_HT", new BigDecimal("20.00")));
        leasing.getLignes().add(new LigneModeleEcritureDTO("44110000", "Société de leasing", "CREDIT", "BASE_TTC", BigDecimal.ZERO));
        MODELES_STANDARDS.put(leasing.getCode(), leasing);

        // 4. Télécommunications & Internet
        ModeleEcritureDTO telecom = new ModeleEcritureDTO(
                "TELECOM_INTERNET",
                "Facture Internet & Téléphone (Maroc Telecom / Inwi / Orange)",
                "Frais télécoms HT (6145), TVA 20% (34552) et opérateur télécom (4411)",
                "AC",
                "EXPLOITATION"
        );
        telecom.getLignes().add(new LigneModeleEcritureDTO("61450000", "Frais de télécommunications", "DEBIT", "BASE_HT", new BigDecimal("20.00")));
        telecom.getLignes().add(new LigneModeleEcritureDTO("34552000", "État - TVA récupérable sur charges", "DEBIT", "TVA_SUR_HT", new BigDecimal("20.00")));
        telecom.getLignes().add(new LigneModeleEcritureDTO("44110000", "Opérateur télécom", "CREDIT", "BASE_TTC", BigDecimal.ZERO));
        MODELES_STANDARDS.put(telecom.getCode(), telecom);

        // 5. Police d'Assurance
        ModeleEcritureDTO assurance = new ModeleEcritureDTO(
                "ASSURANCE_MULTIRISQUE",
                "Primes d'assurance et garanties (exonérées de TVA)",
                "Charge d'assurance (6134) et compagnie d'assurance (4411)",
                "AC",
                "EXPLOITATION"
        );
        assurance.getLignes().add(new LigneModeleEcritureDTO("61340000", "Primes d'assurances", "DEBIT", "BASE_TTC", BigDecimal.ZERO));
        assurance.getLignes().add(new LigneModeleEcritureDTO("44110000", "Compagnie d'assurance", "CREDIT", "BASE_TTC", BigDecimal.ZERO));
        MODELES_STANDARDS.put(assurance.getCode(), assurance);

        // 6. Frais & Commissions Bancaires
        ModeleEcritureDTO banque = new ModeleEcritureDTO(
                "FRAIS_COMMISSIONS_BANQUE",
                "Frais de tenue de compte & commissions bancaires",
                "Commissions HT (6147), TVA 10% sur prestations bancaires (34552) et compte banque (5141)",
                "BQ",
                "BANQUE"
        );
        banque.getLignes().add(new LigneModeleEcritureDTO("61470000", "Services bancaires", "DEBIT", "BASE_HT", new BigDecimal("10.00")));
        banque.getLignes().add(new LigneModeleEcritureDTO("34552000", "État - TVA récupérable sur charges", "DEBIT", "TVA_SUR_HT", new BigDecimal("10.00")));
        banque.getLignes().add(new LigneModeleEcritureDTO("51410000", "Banques", "CREDIT", "BASE_TTC", BigDecimal.ZERO));
        MODELES_STANDARDS.put(banque.getCode(), banque);
    }

    public ModeleEcritureService(ComptabiliteService comptabiliteService) {
        this.comptabiliteService = comptabiliteService;
    }

    /**
     * Liste des modèles standards disponibles
     */
    public List<ModeleEcritureDTO> getModelesDisponibles() {
        return new ArrayList<>(MODELES_STANDARDS.values());
    }

    /**
     * Obtenir le détail d'un modèle
     */
    public ModeleEcritureDTO getModeleByCode(String code) {
        ModeleEcritureDTO m = MODELES_STANDARDS.get(code);
        if (m == null) {
            throw new IllegalArgumentException("Modèle d'écriture introuvable pour le code : " + code);
        }
        return m;
    }

    /**
     * Application instantanée d'un modèle d'écriture :
     * Calcule automatiquement les montants HT, TVA, Retenues et Contreparties,
     * puis génère l'écriture comptable rigoureusement équilibrée.
     */
    public EcritureComptableDTO appliquerModele(AppliquerModeleRequest req) {
        if (req.getCodeModele() == null || req.getCodeModele().trim().isEmpty()) {
            throw new IllegalArgumentException("Le code du modèle est obligatoire.");
        }
        if (req.getMontantBase() == null || req.getMontantBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant de base doit être supérieur à zéro.");
        }

        ModeleEcritureDTO modele = getModeleByCode(req.getCodeModele());

        LocalDate dateEcr = req.getDateEcriture() != null ? req.getDateEcriture() : LocalDate.now();
        String journalCode = (req.getJournalCode() != null && !req.getJournalCode().isEmpty())
                ? req.getJournalCode() : modele.getJournalParDefaut();

        String refPiece = req.getReferencePiece() != null ? req.getReferencePiece() : "MOD-" + System.currentTimeMillis() % 100000;
        String libellePiece = modele.getLibelle();
        if (req.getLibelleComplement() != null && !req.getLibelleComplement().trim().isEmpty()) {
            libellePiece += " - " + req.getLibelleComplement().trim();
        }

        EcritureComptableDTO ecrDto = new EcritureComptableDTO();
        ecrDto.setJournalCode(journalCode);
        ecrDto.setDateEcriture(dateEcr);
        ecrDto.setReferencePiece(refPiece);
        ecrDto.setLibelle(libellePiece);
        ecrDto.setValidee(true);

        List<LigneEcritureDTO> lignes = new ArrayList<>();

        // Calcul des bases HT et TTC
        BigDecimal montantSaisi = req.getMontantBase();
        BigDecimal montantHT;
        BigDecimal montantTTC;

        // Trouver le taux de TVA du modèle s'il existe
        BigDecimal tauxTva = new BigDecimal("20.00");
        for (LigneModeleEcritureDTO l : modele.getLignes()) {
            if ("TVA_SUR_HT".equals(l.getTypeCalcul()) || "BASE_HT".equals(l.getTypeCalcul())) {
                if (l.getTauxOuCoefficient().compareTo(BigDecimal.ZERO) > 0) {
                    tauxTva = l.getTauxOuCoefficient();
                    break;
                }
            }
        }

        BigDecimal facteurTva = BigDecimal.ONE.add(tauxTva.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));

        if (req.isBaseEstTtc()) {
            montantTTC = montantSaisi;
            montantHT = montantTTC.divide(facteurTva, 2, RoundingMode.HALF_UP);
        } else {
            montantHT = montantSaisi;
            montantTTC = montantHT.multiply(facteurTva).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal montantTva = montantTTC.subtract(montantHT);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCreditSansContrepartie = BigDecimal.ZERO;

        LigneModeleEcritureDTO ligneContrepartieSolde = null;

        for (LigneModeleEcritureDTO lMod : modele.getLignes()) {
            if ("SOLDE_CONTREPARTIE".equals(lMod.getTypeCalcul())) {
                ligneContrepartieSolde = lMod;
                continue;
            }

            LigneEcritureDTO l = new LigneEcritureDTO();
            String numCompte = lMod.getNumeroCompte();
            if (numCompte.startsWith("4411") && req.getTiersNumeroCompte() != null && !req.getTiersNumeroCompte().isEmpty()) {
                numCompte = req.getTiersNumeroCompte();
            }
            l.setNumeroCompte(numCompte);
            l.setLibelleLigne(lMod.getLibelleLigne() + " (" + refPiece + ")");
            l.setReferenceLigne(refPiece);

            BigDecimal montantLigne;
            switch (lMod.getTypeCalcul()) {
                case "BASE_HT" -> montantLigne = montantHT;
                case "TVA_SUR_HT" -> montantLigne = montantTva;
                case "BASE_TTC" -> montantLigne = montantTTC;
                case "RETENUE_SOURCE" -> {
                    // RAS calculée sur le montant HT (ex: 10% sur honoraires)
                    BigDecimal tauxRas = lMod.getTauxOuCoefficient().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    montantLigne = montantHT.multiply(tauxRas).setScale(2, RoundingMode.HALF_UP);
                }
                default -> montantLigne = montantHT;
            }

            if ("DEBIT".equalsIgnoreCase(lMod.getSens())) {
                l.setDebit(montantLigne);
                l.setCredit(BigDecimal.ZERO);
                totalDebit = totalDebit.add(montantLigne);
            } else {
                l.setCredit(montantLigne);
                l.setDebit(BigDecimal.ZERO);
                totalCreditSansContrepartie = totalCreditSansContrepartie.add(montantLigne);
            }

            lignes.add(l);
        }

        // Si une ligne est définie par SOLDE_CONTREPARTIE, elle reçoit exactement l'écart pour équilibrer
        if (ligneContrepartieSolde != null) {
            BigDecimal soldeContrepartie = totalDebit.subtract(totalCreditSansContrepartie);
            LigneEcritureDTO lSolde = new LigneEcritureDTO();
            String numCompte = ligneContrepartieSolde.getNumeroCompte();
            if (numCompte.startsWith("4411") && req.getTiersNumeroCompte() != null && !req.getTiersNumeroCompte().isEmpty()) {
                numCompte = req.getTiersNumeroCompte();
            }
            lSolde.setNumeroCompte(numCompte);
            lSolde.setLibelleLigne(ligneContrepartieSolde.getLibelleLigne() + " (" + refPiece + ")");
            lSolde.setReferenceLigne(refPiece);
            lSolde.setCredit(soldeContrepartie);
            lSolde.setDebit(BigDecimal.ZERO);
            lignes.add(lSolde);
        }

        ecrDto.setLignes(lignes);

        // Créer l'écriture via le moteur de comptabilité officiel
        return comptabiliteService.creerEcriture(ecrDto);
    }
}
