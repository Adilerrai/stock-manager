package com.gestion.service;

import com.acommon.persistant.model.TenantContext;
import com.gestion.persistent.dto.*;
import com.gestion.persistent.enums.SensCompte;
import com.gestion.persistent.enums.StatutRapprochement;
import com.gestion.persistent.enums.TypeJournal;
import com.gestion.persistent.model.*;
import com.gestion.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class RapprochementService {

    private static final Logger log = LoggerFactory.getLogger(RapprochementService.class);

    private final ReleveBancaireRepository releveRepository;
    private final LigneReleveBancaireRepository ligneReleveRepository;
    private final CompteFinancierRepository compteFinancierRepository;
    private final CompteComptableRepository compteComptableRepository;
    private final JournalComptableRepository journalRepository;
    private final EcritureComptableRepository ecritureRepository;
    private final LigneEcritureRepository ligneEcritureRepository;
    private final MouvementTresorerieRepository mouvementTresorerieRepository;

    public RapprochementService(ReleveBancaireRepository releveRepository,
            LigneReleveBancaireRepository ligneReleveRepository,
            CompteFinancierRepository compteFinancierRepository,
            CompteComptableRepository compteComptableRepository,
            JournalComptableRepository journalRepository,
            EcritureComptableRepository ecritureRepository,
            LigneEcritureRepository ligneEcritureRepository,
            MouvementTresorerieRepository mouvementTresorerieRepository) {
        this.releveRepository = releveRepository;
        this.ligneReleveRepository = ligneReleveRepository;
        this.compteFinancierRepository = compteFinancierRepository;
        this.compteComptableRepository = compteComptableRepository;
        this.journalRepository = journalRepository;
        this.ecritureRepository = ecritureRepository;
        this.ligneEcritureRepository = ligneEcritureRepository;
        this.mouvementTresorerieRepository = mouvementTresorerieRepository;
    }

    private Long getTenantId() {
        Long t = TenantContext.getCurrentTenant();
        return t != null ? t : 1L;
    }

    // =========================================================================
    // COMPARATIF RAPPROCHEMENT BANQUE ↔ COMPTE 5141
    // =========================================================================

    @Transactional(readOnly = true)
    public RapprochementComparatif5141DTO getComparatif5141(Long compteId, LocalDate dateArrete) {
        return getComparatif5141(compteId, null, dateArrete);
    }

    @Transactional(readOnly = true)
    public RapprochementComparatif5141DTO getComparatif5141(Long compteId, LocalDate dateDebut, LocalDate dateFin) {
        Long tenantId = getTenantId();
        CompteFinancier compte = compteFinancierRepository.findById(compteId)
                .orElseThrow(() -> new IllegalArgumentException("Compte financier introuvable : " + compteId));

        LocalDate dFin = dateFin != null ? dateFin : LocalDate.now();
        LocalDate dDebut = dateDebut != null ? dateDebut : dFin.withDayOfMonth(1);
        if (dDebut.isAfter(dFin)) {
            LocalDate tmp = dDebut;
            dDebut = dFin;
            dFin = tmp;
        }

        RapprochementComparatif5141DTO dto = new RapprochementComparatif5141DTO();
        dto.setCompteId(compte.getId());
        dto.setCompteNom(compte.getNom());
        dto.setNumeroRib(compte.getNumeroCompteRib());
        dto.setNomBanque(compte.getNomBanque() != null ? compte.getNomBanque() : "Banque");
        dto.setDateDebut(dDebut);
        dto.setDateFin(dFin);
        dto.setDateArrete(dFin);

        // Compte comptable 5141
        CompteComptable compte5141 = findCompte5141(tenantId);
        dto.setNumeroCompteComptable(compte5141 != null ? compte5141.getNumeroCompte() : "5141");

        // Solde comptable 5141
        BigDecimal soldeComptable = compte.getSoldeActuel() != null ? compte.getSoldeActuel() : BigDecimal.ZERO;
        dto.setSoldeComptable(soldeComptable);

        // Dernier relevé bancaire pour référence solde
        Optional<ReleveBancaire> dernierReleve = releveRepository
                .findFirstByCompteFinancierIdAndPointDeVenteIdOrderByDateFinDesc(compteId, tenantId);

        BigDecimal soldeReleve = dernierReleve.map(ReleveBancaire::getSoldeFinal).orElse(soldeComptable);
        dto.setSoldeReleve(soldeReleve);
        dto.setEcart(soldeReleve.subtract(soldeComptable));

        // Lignes du relevé bancaire filtrées sur la période [dDebut, dFin]
        List<LigneReleveBancaire> lignesReleve = ligneReleveRepository.findByCompteAndPeriode(compteId, dDebut, dFin,
                tenantId);
        if (lignesReleve.isEmpty()) {
            if (dernierReleve.isPresent()) {
                lignesReleve = dernierReleve.get().getLignes();
            } else {
                lignesReleve = ligneReleveRepository.findNonRapprocheesAvantDate(compteId, dFin, tenantId);
            }
        }

        dto.setTotalLignesReleve(lignesReleve.size());

        // Récupérer les écritures du compte 5141 (avec fenêtre élargie pour inclure les
        // chèques/virements antérieurs en suspens)
        LocalDate debutRechercheCompta = dDebut.minusDays(60);
        LocalDate finRechercheCompta = dFin.plusDays(15);
        List<LigneEcriture> ecritures5141 = ligneEcritureRepository
                .findAllByTenantAndPeriode(tenantId, debutRechercheCompta, finRechercheCompta)
                .stream()
                .filter(l -> l.getCompte() != null && l.getCompte().getNumeroCompte() != null
                        && l.getCompte().getNumeroCompte().startsWith("5141"))
                .collect(Collectors.toList());

        Map<Long, LigneEcriture> mapEcrituresById = ecritures5141.stream()
                .collect(Collectors.toMap(LigneEcriture::getId, e -> e, (e1, e2) -> e1));

        // Déjà pointées en base de données
        List<Long> dejaPointees = ligneReleveRepository.findToutesLignesEcrituresPointees(tenantId);
        Set<Long> ecrituresPointeesIds = new HashSet<>(dejaPointees);

        List<ItemComparatifRapprochementDTO> items = new ArrayList<>();
        int nbRapprochees = 0;
        int nbNonComptabilisees = 0;

        BigDecimal totalDebitsReleveSuspens = BigDecimal.ZERO;
        BigDecimal totalCreditsReleveSuspens = BigDecimal.ZERO;

        for (LigneReleveBancaire l : lignesReleve) {
            ItemComparatifRapprochementDTO item = new ItemComparatifRapprochementDTO();
            item.setLigneReleveId(l.getId());
            item.setDateBanque(l.getDateOperation());
            item.setDateValeurBanque(l.getDateValeur());
            item.setLibelleBanque(l.getLibelle());
            item.setReferenceBanque(l.getReference());
            item.setDebitBanque(l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO);
            item.setCreditBanque(l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO);

            // Suggestions de contrepartie
            SuggetionContrepartie sug = suggererContrepartie(l.getLibelle(), l.getDebit(), l.getCredit());
            item.setSuggestionContrepartieCode(sug.code);
            item.setSuggestionContrepartieLibelle(sug.libelle);

            // Vérifier si déjà pointé avec une ligne d'écriture en base
            if (l.getLigneEcritureId() != null) {
                LigneEcriture e = mapEcrituresById.get(l.getLigneEcritureId());
                if (e == null) {
                    e = ligneEcritureRepository.findById(l.getLigneEcritureId()).orElse(null);
                }
                if (e != null) {
                    ecrituresPointeesIds.add(e.getId());
                    item.setStatut("RAPPROCHE");
                    item.setLigneEcritureId(e.getId());
                    if (e.getEcriture() != null) {
                        item.setEcritureNumeroPiece(e.getEcriture().getNumeroPiece());
                        item.setDateCompta(e.getEcriture().getDateEcriture());
                    }
                    item.setLibelleCompta(e.getLibelleLigne());
                    item.setDebitCompta(e.getDebit() != null ? e.getDebit() : BigDecimal.ZERO);
                    item.setCreditCompta(e.getCredit() != null ? e.getCredit() : BigDecimal.ZERO);
                    nbRapprochees++;
                } else {
                    item.setStatut("NON_COMPTABILISE");
                    nbNonComptabilisees++;
                    totalDebitsReleveSuspens = totalDebitsReleveSuspens.add(item.getDebitBanque());
                    totalCreditsReleveSuspens = totalCreditsReleveSuspens.add(item.getCreditBanque());
                }
            } else if (l.getStatut() == StatutRapprochement.RAPPROCHE) {
                // Rapproché avec un mouvement de trésorerie sans ligne d'écriture directe
                item.setStatut("RAPPROCHE");
                if (l.getMouvementTresorerie() != null) {
                    item.setEcritureNumeroPiece(l.getMouvementTresorerie().getReference());
                    item.setLibelleCompta(l.getMouvementTresorerie().getMotif());
                }
                nbRapprochees++;
            } else {
                // Non rapproché (en suspens bancaire)
                item.setStatut("NON_COMPTABILISE");
                nbNonComptabilisees++;
                totalDebitsReleveSuspens = totalDebitsReleveSuspens.add(item.getDebitBanque());
                totalCreditsReleveSuspens = totalCreditsReleveSuspens.add(item.getCreditBanque());

                // Suggestion de correspondance sans marquer la ligne comme rapprochée
                LigneEcriture match = chercherCorrespondance5141(l, ecritures5141, ecrituresPointeesIds);
                if (match != null) {
                    item.setSuggestionLigneEcritureId(match.getId());
                    if (match.getEcriture() != null) {
                        item.setSuggestionNumeroPiece(match.getEcriture().getNumeroPiece());
                        item.setSuggestionDateCompta(match.getEcriture().getDateEcriture());
                    }
                    item.setSuggestionLibelleCompta(match.getLibelleLigne());
                }
            }

            items.add(item);
        }

        dto.setItems(items);
        dto.setTotalRapprochees(nbRapprochees);
        dto.setTotalNonComptabilisees(nbNonComptabilisees);

        // Écritures du compte 5141 non encore rapprochées (en attente banque)
        List<LigneEcritureSimpleDTO> ecrituresRestantes = ecritures5141.stream()
                .filter(e -> !ecrituresPointeesIds.contains(e.getId()))
                .map(this::toLigneEcritureSimpleDto)
                .collect(Collectors.toList());

        dto.setEcrituresNonPointees(ecrituresRestantes);
        dto.setTotalEnAttenteBanque(ecrituresRestantes.size());

        // Calcul des totaux pour l'État de Rapprochement Bancaire (A, B, C, D, E, F, G,
        // H)
        BigDecimal totalDebitsComptaSuspens = BigDecimal.ZERO;
        BigDecimal totalCreditsComptaSuspens = BigDecimal.ZERO;
        for (LigneEcritureSimpleDTO e : ecrituresRestantes) {
            if (e.getDebit() != null)
                totalDebitsComptaSuspens = totalDebitsComptaSuspens.add(e.getDebit());
            if (e.getCredit() != null)
                totalCreditsComptaSuspens = totalCreditsComptaSuspens.add(e.getCredit());
        }

        dto.setTotalDebitReleveSuspens(totalDebitsReleveSuspens); // B
        dto.setTotalCreditReleveSuspens(totalCreditsReleveSuspens); // C
        dto.setTotalDebitComptaSuspens(totalDebitsComptaSuspens); // D
        dto.setTotalCreditComptaSuspens(totalCreditsComptaSuspens); // E

        // Solde réel en comptabilité (G)
        dto.setSoldeReelComptable(soldeComptable);

        // Solde théorique en comptabilité (F) :
        // Conforme à la cellule N23 de Letat-de-rapprochement-bancaire.xlsx
        // (=H6+F18+M19-G18-L19) :
        // F = A (Solde relevé) + B (Débits relevé) - C (Crédits relevé) - D (Débits
        // compta) + E (Crédits compta)
        BigDecimal sTheorique = soldeReleve
                .add(totalDebitsReleveSuspens) // + B (F18)
                .subtract(totalCreditsReleveSuspens) // - C (G18)
                .subtract(totalDebitsComptaSuspens) // - D (L19)
                .add(totalCreditsComptaSuspens); // + E (M19)
        dto.setSoldeTheoriqueComptable(sTheorique);

        // Écart (H = F - G selon cellule N27 = N23 - N25 de
        // Letat-de-rapprochement-bancaire.xlsx)
        BigDecimal ecartH = sTheorique.subtract(soldeComptable);
        dto.setEcartConcordance(ecartH);

        return dto;
    }

    // =========================================================================
    // CRÉATION D'ÉCRITURE COMPTABLE AUTOMATIQUE (1 CLIC)
    // =========================================================================

    public ItemComparatifRapprochementDTO creerEcriturePourLigne(CreerEcritureReleveRequest req) {
        // L'insertion automatique d'écritures comptables est strictement désactivée
        // selon la règle métier :
        // Le rapprochement bancaire effectue uniquement la liaison (pointage) avec des
        // écritures existantes.
        log.warn(
                "Tentative de création d'écriture ignorée : l'insertion dans les écritures comptables est désactivée.");
        throw new UnsupportedOperationException(
                "L'insertion automatique d'écriture comptable lors du rapprochement bancaire est désactivée. Le rapprochement effectue uniquement la liaison (pointage) avec des écritures comptables existantes.");
    }

    // =========================================================================
    // AUTO-RAPPROCHEMENT AUTOMATIQUE INTELLIGENT
    // =========================================================================

    public Map<String, Object> autoRapprocher5141(Long releveId) {
        Long tenantId = getTenantId();
        ReleveBancaire releve = releveRepository.findByIdAndPointDeVenteId(releveId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Relevé introuvable : " + releveId));

        List<LigneReleveBancaire> nonRapprochees = releve.getLignes().stream()
                .filter(l -> l.getStatut() == StatutRapprochement.NON_RAPPROCHE)
                .collect(Collectors.toList());

        LocalDate debut = releve.getDateDebut() != null ? releve.getDateDebut().minusDays(15)
                : LocalDate.now().minusMonths(1);
        LocalDate fin = releve.getDateFin() != null ? releve.getDateFin().plusDays(15) : LocalDate.now().plusDays(15);

        List<LigneEcriture> ecritures5141 = ligneEcritureRepository.findAllByTenantAndPeriode(tenantId, debut, fin)
                .stream()
                .filter(l -> l.getCompte() != null && l.getCompte().getNumeroCompte() != null
                        && l.getCompte().getNumeroCompte().startsWith("5141"))
                .collect(Collectors.toList());

        List<Long> dejaPointees = ligneReleveRepository.findToutesLignesEcrituresPointees(tenantId);
        Set<Long> dejaUtilisees = new HashSet<>(dejaPointees);

        int count = 0;
        for (LigneReleveBancaire l : nonRapprochees) {
            LigneEcriture match = chercherCorrespondance5141(l, ecritures5141, dejaUtilisees);
            if (match != null) {
                l.setLigneEcritureId(match.getId());
                l.setStatut(StatutRapprochement.RAPPROCHE);
                l.setDateRapprochement(LocalDateTime.now());
                ligneReleveRepository.save(l);
                dejaUtilisees.add(match.getId());
                count++;
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("reconcilies", count);
        res.put("totalLignes", releve.getLignes().size());
        res.put("restantesNonComptabilisees", releve.getLignes().size() - dejaUtilisees.size());
        return res;
    }

    // =========================================================================
    // UTILITAIRES DE RECHERCHE ET SUGGESTIONS
    // =========================================================================

    private LigneEcriture chercherCorrespondance5141(LigneReleveBancaire l, List<LigneEcriture> pool,
            Set<Long> exclus) {
        BigDecimal montantRecherche;
        boolean banqueEstDebit = l.getDebit().compareTo(BigDecimal.ZERO) > 0;

        if (banqueEstDebit) {
            // Sortie d'argent banque : cherche Crédit en 5141
            montantRecherche = l.getDebit();
        } else {
            // Entrée d'argent banque : cherche Débit en 5141
            montantRecherche = l.getCredit();
        }

        for (LigneEcriture e : pool) {
            if (exclus.contains(e.getId()))
                continue;

            BigDecimal montantCompta = banqueEstDebit ? e.getCredit() : e.getDebit();
            if (montantCompta != null && montantCompta.compareTo(montantRecherche) == 0) {
                LocalDate dateEcriture = e.getEcriture() != null ? e.getEcriture().getDateEcriture() : null;
                if (dateEcriture != null && l.getDateOperation() != null) {
                    long delta = Math.abs(ChronoUnit.DAYS.between(l.getDateOperation(), dateEcriture));
                    if (delta <= 15) {
                        return e;
                    }
                }
            }
        }
        return null;
    }

    private CompteComptable findCompte5141(Long tenantId) {
        List<CompteComptable> comptes = compteComptableRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
        return comptes.stream()
                .filter(c -> c.getNumeroCompte() != null && c.getNumeroCompte().startsWith("5141"))
                .findFirst()
                .orElse(null);
    }



    private static class SuggetionContrepartie {
        String code;
        String libelle;

        SuggetionContrepartie(String code, String libelle) {
            this.code = code;
            this.libelle = libelle;
        }
    }

    private SuggetionContrepartie suggererContrepartie(String libelle, BigDecimal debit, BigDecimal credit) {
        String upper = libelle != null ? libelle.toUpperCase() : "";

        if (upper.contains("FRAIS") || upper.contains("COMMISSION") || upper.contains("COTISATION")
                || upper.contains("AGIOS") || upper.contains("TENUE DE COMPTE")) {
            return new SuggetionContrepartie("6147", "Services bancaires et assimilés");
        }
        if (upper.contains("VIR CLIENT") || upper.contains("VIREMENT RECU") || upper.contains("REMISE")
                || upper.contains("ENCAISSEMENT")) {
            return new SuggetionContrepartie("3421", "Clients");
        }
        if (upper.contains("VIR FOURNISSEUR") || upper.contains("PRELEVEMENT") || upper.contains("PAIEMENT")
                || upper.contains("CHQ") || upper.contains("FOURN")) {
            return new SuggetionContrepartie("4411", "Fournisseurs");
        }
        if (upper.contains("SALAIRE") || upper.contains("PAIE") || upper.contains("CNSS")) {
            return new SuggetionContrepartie("4432", "Rémunérations dues au personnel");
        }
        if (upper.contains("LOYER")) {
            return new SuggetionContrepartie("6131", "Locations et charges locatives");
        }
        if (upper.contains("TELECOM") || upper.contains("IAM") || upper.contains("ORANGE") || upper.contains("INWI")) {
            return new SuggetionContrepartie("6145", "Frais postaux et de télécommunications");
        }

        // Par défaut selon le sens financier
        if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
            return new SuggetionContrepartie("3421", "Clients (Règlement client)");
        } else {
            return new SuggetionContrepartie("6147", "Services bancaires / Frais");
        }
    }

    private LigneEcritureSimpleDTO toLigneEcritureSimpleDto(LigneEcriture l) {
        LigneEcritureSimpleDTO dto = new LigneEcritureSimpleDTO();
        dto.setId(l.getId());
        if (l.getEcriture() != null) {
            dto.setEcritureId(l.getEcriture().getId());
            dto.setNumeroPiece(l.getEcriture().getNumeroPiece());
            dto.setDateEcriture(l.getEcriture().getDateEcriture());
        }
        if (l.getCompte() != null) {
            dto.setNumeroCompte(l.getCompte().getNumeroCompte());
        }
        dto.setLibelleLigne(l.getLibelleLigne());
        dto.setDebit(l.getDebit());
        dto.setCredit(l.getCredit());
        dto.setReferenceLigne(l.getReferenceLigne());
        return dto;
    }
}
