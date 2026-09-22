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
        List<LigneReleveBancaire> lignesReleve = ligneReleveRepository.findByCompteAndPeriode(compteId, dDebut, dFin, tenantId);
        if (lignesReleve.isEmpty()) {
            if (dernierReleve.isPresent()) {
                lignesReleve = dernierReleve.get().getLignes();
            } else {
                lignesReleve = ligneReleveRepository.findNonRapprocheesAvantDate(compteId, dFin, tenantId);
            }
        }

        dto.setTotalLignesReleve(lignesReleve.size());

        // Récupérer les écritures du compte 5141 sur la période [dDebut, dFin]
        List<LigneEcriture> ecritures5141 = ligneEcritureRepository.findAllByTenantAndPeriode(tenantId, dDebut, dFin)
                .stream()
                .filter(l -> l.getCompte() != null && l.getCompte().getNumeroCompte() != null && l.getCompte().getNumeroCompte().startsWith("5141"))
                .collect(Collectors.toList());

        Map<Long, LigneEcriture> mapEcrituresById = ecritures5141.stream()
                .collect(Collectors.toMap(LigneEcriture::getId, e -> e, (e1, e2) -> e1));

        Set<Long> ecrituresPointeesIds = new HashSet<>();
        List<ItemComparatifRapprochementDTO> items = new ArrayList<>();
        int nbRapprochees = 0;
        int nbNonComptabilisees = 0;

        for (LigneReleveBancaire l : lignesReleve) {
            ItemComparatifRapprochementDTO item = new ItemComparatifRapprochementDTO();
            item.setLigneReleveId(l.getId());
            item.setDateBanque(l.getDateOperation());
            item.setDateValeurBanque(l.getDateValeur());
            item.setLibelleBanque(l.getLibelle());
            item.setReferenceBanque(l.getReference());
            item.setDebitBanque(l.getDebit());
            item.setCreditBanque(l.getCredit());

            // Suggestions automatiques de contrepartie
            SuggetionContrepartie sug = suggererContrepartie(l.getLibelle(), l.getDebit(), l.getCredit());
            item.setSuggestionContrepartieCode(sug.code);
            item.setSuggestionContrepartieLibelle(sug.libelle);

            // Vérifier si déjà pointé avec une ligne d'écriture
            if (l.getLigneEcritureId() != null && mapEcrituresById.containsKey(l.getLigneEcritureId())) {
                LigneEcriture e = mapEcrituresById.get(l.getLigneEcritureId());
                ecrituresPointeesIds.add(e.getId());

                item.setStatut("RAPPROCHE");
                item.setLigneEcritureId(e.getId());
                if (e.getEcriture() != null) {
                    item.setEcritureNumeroPiece(e.getEcriture().getNumeroPiece());
                    item.setDateCompta(e.getEcriture().getDateEcriture());
                }
                item.setLibelleCompta(e.getLibelleLigne());
                item.setDebitCompta(e.getDebit());
                item.setCreditCompta(e.getCredit());
                nbRapprochees++;
            } else if (l.getStatut() == StatutRapprochement.RAPPROCHE) {
                // Rapproché avec un mouvement de trésorerie sans ligne d'écriture directe
                item.setStatut("RAPPROCHE");
                if (l.getMouvementTresorerie() != null) {
                    item.setEcritureNumeroPiece(l.getMouvementTresorerie().getReference());
                    item.setLibelleCompta(l.getMouvementTresorerie().getMotif());
                }
                nbRapprochees++;
            } else {
                // Recherche d'une correspondance automatique potentielle
                LigneEcriture match = chercherCorrespondance5141(l, ecritures5141, ecrituresPointeesIds);
                if (match != null) {
                    // Match trouvé non encore explicitement sauvegardé
                    item.setStatut("RAPPROCHE");
                    item.setLigneEcritureId(match.getId());
                    if (match.getEcriture() != null) {
                        item.setEcritureNumeroPiece(match.getEcriture().getNumeroPiece());
                        item.setDateCompta(match.getEcriture().getDateEcriture());
                    }
                    item.setLibelleCompta(match.getLibelleLigne());
                    item.setDebitCompta(match.getDebit());
                    item.setCreditCompta(match.getCredit());
                    ecrituresPointeesIds.add(match.getId());
                    nbRapprochees++;
                } else {
                    // ⚠ Non comptabilisé !
                    item.setStatut("NON_COMPTABILISE");
                    nbNonComptabilisees++;
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

        return dto;
    }

    // =========================================================================
    // CRÉATION D'ÉCRITURE COMPTABLE AUTOMATIQUE (1 CLIC)
    // =========================================================================

    public ItemComparatifRapprochementDTO creerEcriturePourLigne(CreerEcritureReleveRequest req) {
        Long tenantId = getTenantId();
        if (req.getLigneReleveId() == null) {
            throw new IllegalArgumentException("L'identifiant de la ligne de relevé est obligatoire.");
        }

        LigneReleveBancaire ligne = ligneReleveRepository.findById(req.getLigneReleveId())
                .orElseThrow(() -> new IllegalArgumentException("Ligne de relevé introuvable : " + req.getLigneReleveId()));

        CompteComptable compte5141 = findCompte5141(tenantId);
        if (compte5141 == null) {
            compte5141 = creerCompteDefaut5141(tenantId);
        }

        // Trouver ou créer le compte de contrepartie
        CompteComptable contrepartie = resoudreCompteContrepartie(req, ligne, tenantId);

        // Trouver le journal de Banque ("BQ")
        JournalComptable journalBq = journalRepository.findByCodeAndPointDeVenteId("BQ", tenantId)
                .orElseGet(() -> {
                    JournalComptable j = new JournalComptable("BQ", "Journal de Banque", TypeJournal.BANQUE, tenantId);
                    return journalRepository.save(j);
                });

        LocalDate dateEcriture = ligne.getDateOperation() != null ? ligne.getDateOperation() : LocalDate.now();
        String libelle = req.getLibelle() != null && !req.getLibelle().isBlank()
                ? req.getLibelle()
                : ligne.getLibelle();

        EcritureComptable ecriture = new EcritureComptable();
        ecriture.setJournal(journalBq);
        ecriture.setDateEcriture(dateEcriture);
        ecriture.setLibelle(libelle);
        ecriture.setReferencePiece(ligne.getReference() != null ? ligne.getReference() : "REL-OP-" + ligne.getId());
        ecriture.setValidee(true);
        ecriture.setPointDeVenteId(tenantId);
        ecriture.setNumeroPiece("BQ-" + dateEcriture.getYear() + "-" + String.format("%05d", System.currentTimeMillis() % 100000));

        BigDecimal montantDebit = ligne.getDebit() != null ? ligne.getDebit() : BigDecimal.ZERO;
        BigDecimal montantCredit = ligne.getCredit() != null ? ligne.getCredit() : BigDecimal.ZERO;

        LigneEcriture ligneBanque5141;

        if (montantDebit.compareTo(BigDecimal.ZERO) > 0) {
            // Sortie d'argent à la banque (Débit relevé) :
            // Comptabilité : Débit Contrepartie (ex: 6147 / 4411) / Crédit 5141 (Banque)
            LigneEcriture lContrepartie = new LigneEcriture(contrepartie, montantDebit, BigDecimal.ZERO, libelle, tenantId);
            ligneBanque5141 = new LigneEcriture(compte5141, BigDecimal.ZERO, montantDebit, libelle, tenantId);

            ecriture.addLigne(lContrepartie);
            ecriture.addLigne(ligneBanque5141);
        } else {
            // Entrée d'argent à la banque (Crédit relevé) :
            // Comptabilité : Débit 5141 (Banque) / Crédit Contrepartie (ex: 3421 Client)
            ligneBanque5141 = new LigneEcriture(compte5141, montantCredit, BigDecimal.ZERO, libelle, tenantId);
            LigneEcriture lContrepartie = new LigneEcriture(contrepartie, BigDecimal.ZERO, montantCredit, libelle, tenantId);

            ecriture.addLigne(ligneBanque5141);
            ecriture.addLigne(lContrepartie);
        }

        ecriture.recalculerTotaux();
        EcritureComptable savedEcriture = ecritureRepository.save(ecriture);

        // Mettre à jour la ligne de relevé bancaire en RAPPROCHE
        ligne.setLigneEcritureId(ligneBanque5141.getId());
        ligne.setStatut(StatutRapprochement.RAPPROCHE);
        ligne.setDateRapprochement(LocalDateTime.now());
        LigneReleveBancaire savedLigne = ligneReleveRepository.save(ligne);

        log.info("Écriture {} créée pour la ligne {} : statut -> RAPPROCHE", savedEcriture.getNumeroPiece(), savedLigne.getId());

        // Retourner le résultat comparatif mis à jour
        ItemComparatifRapprochementDTO res = new ItemComparatifRapprochementDTO();
        res.setLigneReleveId(savedLigne.getId());
        res.setDateBanque(savedLigne.getDateOperation());
        res.setDateValeurBanque(savedLigne.getDateValeur());
        res.setLibelleBanque(savedLigne.getLibelle());
        res.setReferenceBanque(savedLigne.getReference());
        res.setDebitBanque(savedLigne.getDebit());
        res.setCreditBanque(savedLigne.getCredit());
        res.setStatut("RAPPROCHE");
        res.setLigneEcritureId(ligneBanque5141.getId());
        res.setEcritureNumeroPiece(savedEcriture.getNumeroPiece());
        res.setDateCompta(savedEcriture.getDateEcriture());
        res.setLibelleCompta(ligneBanque5141.getLibelleLigne());
        res.setDebitCompta(ligneBanque5141.getDebit());
        res.setCreditCompta(ligneBanque5141.getCredit());
        res.setSuggestionContrepartieCode(contrepartie.getNumeroCompte());
        res.setSuggestionContrepartieLibelle(contrepartie.getLibelle());

        return res;
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

        LocalDate debut = releve.getDateDebut() != null ? releve.getDateDebut().minusDays(15) : LocalDate.now().minusMonths(1);
        LocalDate fin = releve.getDateFin() != null ? releve.getDateFin().plusDays(15) : LocalDate.now().plusDays(15);

        List<LigneEcriture> ecritures5141 = ligneEcritureRepository.findAllByTenantAndPeriode(tenantId, debut, fin)
                .stream()
                .filter(l -> l.getCompte() != null && l.getCompte().getNumeroCompte() != null && l.getCompte().getNumeroCompte().startsWith("5141"))
                .collect(Collectors.toList());

        Set<Long> dejaUtilisees = releve.getLignes().stream()
                .filter(l -> l.getLigneEcritureId() != null)
                .map(LigneReleveBancaire::getLigneEcritureId)
                .collect(Collectors.toSet());

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

    private LigneEcriture chercherCorrespondance5141(LigneReleveBancaire l, List<LigneEcriture> pool, Set<Long> exclus) {
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
            if (exclus.contains(e.getId())) continue;

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

    private CompteComptable creerCompteDefaut5141(Long tenantId) {
        CompteComptable c = new CompteComptable("51410000", "Banques (comptes en monnaie nationale)", 5, SensCompte.DEBIT, tenantId);
        return compteComptableRepository.save(c);
    }

    private CompteComptable resoudreCompteContrepartie(CreerEcritureReleveRequest req, LigneReleveBancaire ligne, Long tenantId) {
        if (req.getCompteContrepartieId() != null) {
            return compteComptableRepository.findById(req.getCompteContrepartieId())
                    .orElseThrow(() -> new IllegalArgumentException("Compte contrepartie introuvable : " + req.getCompteContrepartieId()));
        }

        String code = req.getNumeroCompteContrepartie();
        if (code == null || code.isBlank()) {
            SuggetionContrepartie sug = suggererContrepartie(ligne.getLibelle(), ligne.getDebit(), ligne.getCredit());
            code = sug.code;
        }

        String finalCode = code.trim();
        List<CompteComptable> existants = compteComptableRepository.findByPointDeVenteIdOrderByNumeroCompteAsc(tenantId);
        Optional<CompteComptable> opt = existants.stream()
                .filter(c -> c.getNumeroCompte() != null && (c.getNumeroCompte().equals(finalCode) || c.getNumeroCompte().startsWith(finalCode)))
                .findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }

        // Créer le compte s'il n'existe pas encore
        SuggetionContrepartie sug = suggererContrepartie(ligne.getLibelle(), ligne.getDebit(), ligne.getCredit());
        int classe = Character.getNumericValue(finalCode.charAt(0));
        SensCompte sens = classe >= 6 ? SensCompte.DEBIT : (classe == 4 ? SensCompte.CREDIT : SensCompte.DEBIT);
        CompteComptable nouveau = new CompteComptable(finalCode, sug.libelle, classe, sens, tenantId);
        return compteComptableRepository.save(nouveau);
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

        if (upper.contains("FRAIS") || upper.contains("COMMISSION") || upper.contains("COTISATION") || upper.contains("AGIOS") || upper.contains("TENUE DE COMPTE")) {
            return new SuggetionContrepartie("6147", "Services bancaires et assimilés");
        }
        if (upper.contains("VIR CLIENT") || upper.contains("VIREMENT RECU") || upper.contains("REMISE") || upper.contains("ENCAISSEMENT")) {
            return new SuggetionContrepartie("3421", "Clients");
        }
        if (upper.contains("VIR FOURNISSEUR") || upper.contains("PRELEVEMENT") || upper.contains("PAIEMENT") || upper.contains("CHQ") || upper.contains("FOURN")) {
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
