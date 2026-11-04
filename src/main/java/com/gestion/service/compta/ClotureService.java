package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.ClotureDtos.AffectationLigneRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.AffectationRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreteDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ArreterRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureDefinitiveDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.ClotureExerciceDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.CloturerRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.EtapeClotureDTO;
import com.gestion.persistent.dto.compta.ClotureDtos.LeverArreteRequest;
import com.gestion.persistent.dto.compta.ClotureDtos.StatutExerciceDTO;
import com.gestion.persistent.dto.compta.EcritureDtos.EcritureDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RapportControlesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ResultatControleDTO;
import com.gestion.persistent.dto.compta.LivreDtos.ExerciceDatesRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.JournalDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.PeriodeDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.BrouillonRepository;
import com.gestion.repository.compta.ClotureRepository;
import com.gestion.repository.compta.ClotureRepository.EtatFige;
import com.gestion.repository.compta.ClotureRepository.SoldeGestion;
import com.gestion.repository.compta.ControleRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.JournalRepository;
import com.gestion.repository.compta.JournalRepository.ANouveauEcrit;
import com.gestion.repository.compta.JournalRepository.EcritureEnregistree;
import com.gestion.repository.compta.JournalRepository.LigneJournal;
import com.gestion.repository.compta.JournalRepository.NouvelleEcriture;
import com.gestion.repository.compta.JournalRepository.NouvelleLigne;
import com.gestion.repository.compta.JournauxRepository;
import com.gestion.repository.compta.MesuresRepository;
import com.gestion.repository.compta.PeriodeRepository;
import com.gestion.repository.compta.PeriodeRepository.LignePeriode;
import com.gestion.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le passage d'un exercice à l'autre. L'arrêté se lève ; la clôture définitive ne se défait pas et tient en une seule transaction :
 * un exercice ne peut pas se retrouver clos sans ses à-nouveaux. L'archive vient après, hors transaction, et se rejoue.
 */
@Service
public class ClotureService {

    private static final Logger LOG = LoggerFactory.getLogger(ClotureService.class);
    private static final String ARCHIVE_FEC = "FEC";
    private static final String ARCHIVE_ETATS = "ETATS";
    private static final List<String> COMPTES_BENEFICE = List.of("114", "115", "116", "118", "4465");
    private static final List<String> COMPTES_PERTE = List.of("1169", "1189");
    private static final BigDecimal Z = BigDecimal.ZERO;

    private final ExerciceRepository exercices;
    private final ExerciceService exerciceService;
    private final PeriodeRepository periodes;
    private final JournalRepository journal;
    private final JournauxRepository journaux;
    private final BrouillonRepository brouillons;
    private final ClotureRepository clotures;
    private final ControleRepository controleRepository;
    private final MesuresRepository mesures;
    private final PosteurEcritures posteur;
    private final EtatService etats;
    private final EtatExports exports;
    private final ControleService controles;
    private final FecService fec;
    private final LivreCourantService livreCourant;
    private final AuditService audit;
    private final TransactionTemplate transaction;

    private final InventaireClotureService inventaires;
    private final IsComptaService impots;
    private final ImmobilisationComptaService immobilisations;

    public ClotureService(ExerciceRepository exercices, ExerciceService exerciceService, PeriodeRepository periodes, JournalRepository journal, JournauxRepository journaux,
                          BrouillonRepository brouillons, ClotureRepository clotures, ControleRepository controleRepository, MesuresRepository mesures, PosteurEcritures posteur,
                          EtatService etats, EtatExports exports, ControleService controles, FecService fec, LivreCourantService livreCourant, AuditService audit,
                          PlatformTransactionManager transactions, InventaireClotureService inventaires, ImmobilisationComptaService immobilisations, @org.springframework.context.annotation.Lazy IsComptaService impots) {
        this.inventaires = inventaires;
        this.impots = impots;
        this.immobilisations = immobilisations;
        this.exercices = exercices;
        this.exerciceService = exerciceService;
        this.periodes = periodes;
        this.journal = journal;
        this.journaux = journaux;
        this.brouillons = brouillons;
        this.clotures = clotures;
        this.controleRepository = controleRepository;
        this.mesures = mesures;
        this.posteur = posteur;
        this.etats = etats;
        this.exports = exports;
        this.controles = controles;
        this.fec = fec;
        this.livreCourant = livreCourant;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Transactional(readOnly = true)
    public ClotureExerciceDTO etat(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        List<PeriodeDTO> liste = periodes.lister(livreId, exerciceId);
        long courantes = liste.stream().filter(p -> "COURANTE".equals(p.typePeriode())).count();
        long closes = liste.stream().filter(p -> "COURANTE".equals(p.typePeriode()) && "CLOTUREE".equals(p.statut())).count();
        long enCours = brouillons.compter(livreId, exercice.du(), exercice.au());
        boolean ouvert = "OUVERT".equals(exercice.statut());
        boolean clos = "CLOTURE".equals(exercice.statut());

        List<EtapeClotureDTO> etapes = new ArrayList<>();
        etapes.add(new EtapeClotureDTO("PERIODES", closes == courantes ? "FAIT" : "A_FAIRE", closes + " périodes sur " + courantes + " clôturées"));
        etapes.add(new EtapeClotureDTO("BROUILLONS", enCours == 0 ? "FAIT" : "A_FAIRE", enCours == 0 ? "Aucun brouillard" : enCours + " brouillard(s) à valider ou supprimer"));
        boolean inventaireValide = inventaires.estValide(livreId, exerciceId);
        etapes.add(new EtapeClotureDTO("INVENTAIRE", inventaireValide ? "FAIT" : "A_FAIRE", inventaireValide ? "Inventaire validé" : "Validez l'inventaire, ou déclarez qu'il n'y a aucun stock"));
        long manquantes = immobilisations.dotationsManquantes(livreId, exercice);
        etapes.add(new EtapeClotureDTO("DOTATIONS", manquantes == 0 ? "FAIT" : "A_FAIRE", manquantes == 0 ? "Aucune dotation en attente" : manquantes + " immobilisation(s) sans dotation : passez-les, ou donnez un motif à l'arrêté"));
        boolean isValide = impots.estValide(livreId, exerciceId);
        etapes.add(new EtapeClotureDTO("IS", isValide ? "FAIT" : ouvert ? "A_FAIRE" : "NON_EFFECTUE", isValide ? "IS validé" : "Calculez et validez l'IS, ou donnez un motif à l'arrêté"));
        etapes.add(etapeControles(exerciceId));
        Optional<String> sha = clotures.empreinteArchive(livreId, exerciceId, ARCHIVE_FEC);
        if (clos) {
            etapes.add(new EtapeClotureDTO("ARCHIVE", sha.isPresent() ? "FAIT" : "A_FAIRE", sha.map(s -> "FEC, empreinte " + s.substring(0, 12) + "…").orElse("Archive absente : rejouez-la")));
        }
        return new ClotureExerciceDTO(exercice.statut(), etapes, ouvert && blocageArrete(livreId, exercice).isEmpty(), "ARRETE".equals(exercice.statut()),
                "ARRETE".equals(exercice.statut()), exercice.resultatNet(), clos ? resteAAffecter(livreId, exercice) : null, sha.orElse(null));
    }

    private EtapeClotureDTO etapeControles(Long exerciceId) {
        RapportControlesDTO rapport = controles.dernier(exerciceId);
        if (rapport.execution() == null) {
            return new EtapeClotureDTO("CONTROLES", "NON_EFFECTUE", "Pas encore rejoués ; l'arrêté les rejoue");
        }
        return rapport.bloquants() > 0
                ? new EtapeClotureDTO("CONTROLES", "A_FAIRE", rapport.bloquants() + " échec(s) bloquant(s)")
                : new EtapeClotureDTO("CONTROLES", "FAIT", "Aucun échec bloquant à la dernière exécution");
    }

    /** Ce qui empêche l'arrêté, dit dans les mots du comptable. La base refuse de toute façon les mêmes cas. */
    private Optional<String> blocageArrete(Long livreId, ExerciceDTO exercice) {
        if (exercice.precedentId() != null) {
            ExerciceDTO precedent = exiger(livreId, exercice.precedentId());
            if (!"CLOTURE".equals(precedent.statut())) {
                return Optional.of("L'exercice " + precedent.code() + " n'est pas clôturé : il passe d'abord.");
            }
        }
        long ouvertes = periodes.lister(livreId, exercice.id()).stream().filter(p -> "COURANTE".equals(p.typePeriode()) && !"CLOTUREE".equals(p.statut())).count();
        if (ouvertes > 0) {
            return Optional.of(ouvertes + " période(s) courante(s) restent à clôturer.");
        }
        long enCours = brouillons.compter(livreId, exercice.du(), exercice.au());
        if (enCours > 0) {
            return Optional.of(enCours + " brouillard(s) restent : validez-les ou supprimez-les.");
        }
        if (!inventaires.estValide(livreId, exercice.id())) {
            return Optional.of("L'inventaire n'est pas validé : validez-le, ou déclarez qu'il n'y a aucun stock.");
        }
        return Optional.empty();
    }

    /** Les contrôles se rejouent à part : leur trace reste, même quand l'arrêté est refusé. */
    public ArreteDTO arreter(Long exerciceId, ArreterRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        if (!"OUVERT".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " n'est pas ouvert.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_OUVERT");
        }
        Optional<String> blocage = blocageArrete(livreId, exercice);
        if (blocage.isPresent()) {
            throw new CommonException(blocage.get(), HttpStatus.CONFLICT, "COMPTA_ARRETE_IMPOSSIBLE");
        }
        long manquantes = immobilisations.dotationsManquantes(livreId, exercice);
        String motifDotations = demande.motifDotations() == null ? "" : demande.motifDotations().trim();
        if (manquantes > 0 && motifDotations.isEmpty()) {
            throw new CommonException(manquantes + " immobilisation(s) sans dotation : passez les dotations, ou donnez un motif.", HttpStatus.CONFLICT, "COMPTA_ARRETE_IMPOSSIBLE");
        }
        String motifIs = demande.motifIs() == null ? "" : demande.motifIs().trim();
        if (!impots.estValide(livreId, exerciceId) && motifIs.isEmpty()) {
            throw new CommonException("L'IS de " + exercice.code() + " n'est pas validé : validez-le, ou donnez un motif.", HttpStatus.CONFLICT, "COMPTA_ARRETE_IMPOSSIBLE");
        }
        RapportControlesDTO rapport = controles.rejouer(exerciceId);
        if (rapport.bloquants() > 0) {
            StringBuilder liste = new StringBuilder(rapport.bloquants() + " contrôle(s) bloquant(s) :");
            for (ResultatControleDTO r : rapport.resultats()) {
                if (Boolean.FALSE.equals(r.reussi()) && "BLOQUANT".equals(r.gravite())) {
                    liste.append(' ').append(r.code()).append(r.message() != null ? " (" + r.message() + ")" : "").append(';');
                }
            }
            throw new CommonException(liste.toString(), HttpStatus.CONFLICT, "COMPTA_CONTROLES_BLOQUANTS");
        }
        return transaction.execute(statut -> {
            clotures.verrouiller(livreId, exerciceId);
            int figeage = clotures.dernierFigeage(livreId, exerciceId) + 1;
            String auteur = livreCourant.auteur();
            for (String code : EtatService.ETATS_SYNTHESE) {
                EtatDTO calcule = etats.calculer(livreId, code, exercice, exercice.au());
                clotures.figer(livreId, exerciceId, figeage, code, calcule.versionDefinition(), etats.contenu(calcule), etats.empreinte(calcule), auteur);
            }
            BigDecimal resultat = etats.resultatNet(livreId, exercice);
            clotures.arreter(livreId, exerciceId, figeage, resultat, auteur);
            audit.logAction(ActionAudit.CLOTURE, "compta_exercices", exerciceId, tronquer("Arrêté de " + exercice.code() + ", figeage " + figeage
                    + (motifIs.isEmpty() ? "" : ". IS non validé : " + motifIs) + (manquantes > 0 ? ". Dotations non passées : " + motifDotations : "")));
            return new ArreteDTO("ARRETE", figeage, resultat);
        });
    }

    /** Rien n'est supprimé : les états figés restent, et aucune écriture n'ayant été passée à l'arrêté, il n'y a rien à annuler. */
    @Transactional
    public StatutExerciceDTO leverArrete(Long exerciceId, LeverArreteRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        if (!"ARRETE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " n'est pas arrêté.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_ARRETE");
        }
        clotures.verrouiller(livreId, exerciceId);
        clotures.leverArrete(livreId, exerciceId);
        audit.logAction(ActionAudit.MODIFICATION, "compta_exercices", exerciceId, tronquer("Arrêté de " + exercice.code() + " levé : " + demande.motif().trim()));
        return new StatutExerciceDTO("OUVERT");
    }

    public ClotureDefinitiveDTO cloturer(Long exerciceId, CloturerRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        if (!exercice.code().equals(demande.confirmation().trim())) {
            throw new CommonException("Saisissez le code de l'exercice (" + exercice.code() + ") pour confirmer : cette opération est sans retour.",
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_CONFIRMATION");
        }
        if ("ARRETE".equals(exercice.statut())) {
            refuserSiBloqueLaCloture(exerciceId);
        }
        ClotureDefinitiveDTO resultat = transaction.execute(statut -> clotureDefinitive(livreId, exercice));
        String sha = null;
        try {
            sha = archiver(livreId, exerciceId);
        } catch (RuntimeException e) {
            // L'exercice reste clos : le contrôle ARCHIVE_ABSENTE le signale et l'archive se rejoue.
            LOG.error("Archive de clôture non produite pour l'exercice {}", exerciceId, e);
        }
        return new ClotureDefinitiveDTO(resultat.statut(), resultat.ecritureClotureId(), resultat.ecritureOuvertureId(), resultat.exerciceSuivantId(),
                resultat.lignesAnouveau(), resultat.extournes(), sha);
    }

    /** Les comptes à solder, les négatifs interdits et les sens anormaux du bilan n'empêchent pas l'arrêté : ils empêchent de clore. */
    private void refuserSiBloqueLaCloture(Long exerciceId) {
        StringBuilder liste = new StringBuilder();
        for (ResultatControleDTO r : controles.rejouer(exerciceId).resultats()) {
            if (Boolean.FALSE.equals(r.reussi()) && !"AVERTISSEMENT".equals(r.gravite())) {
                liste.append(liste.isEmpty() ? "" : " ").append(r.message() != null ? r.message() : r.code());
            }
        }
        if (!liste.isEmpty()) {
            throw new CommonException("Clôture impossible : " + liste, HttpStatus.CONFLICT, "COMPTA_CONTROLES_BLOQUE_CLOTURE");
        }
    }

    private ClotureDefinitiveDTO clotureDefinitive(Long livreId, ExerciceDTO lu) {
        String auteur = livreCourant.auteur();
        clotures.verrouiller(livreId, lu.id());
        ExerciceDTO exercice = exiger(livreId, lu.id());
        if ("CLOTURE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " est déjà clôturé.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_CLOS");
        }
        if (!"ARRETE".equals(exercice.statut())) {
            throw new CommonException("Arrêtez d'abord l'exercice " + exercice.code() + " : la clôture définitive part des états figés.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_ARRETE");
        }
        verifierEtatsFiges(livreId, exercice);

        periodes.ouvrirClotureSiPossible(livreId, exercice.id());
        LignePeriode periodeCloture = periodes.specialePourDate(livreId, "CLOTURE", exercice.au()).orElseThrow();
        Long ecritureCloture = passerEcritureCloture(livreId, exercice);

        ExerciceDTO suivant = exerciceSuivant(livreId, exercice, auteur);
        LignePeriode ouverture = periodes.specialePourDate(livreId, "OUVERTURE", suivant.du()).orElseThrow();
        JournalDTO journalAn = journaux.parType(livreId, "A_NOUVEAUX").orElseThrow();
        LocalDateTime maintenant = LocalDateTime.now();
        Optional<ANouveauEcrit> aNouveau = journal.ajouterANouveau(livreId, exercice.id(), new NouvelleEcriture(suivant.id(), ouverture.id(), journalAn.id(), journalAn.typeJournal(),
                suivant.du(), suivant.du(), "AN-" + suivant.code(), "À-nouveaux de l'exercice " + suivant.code(), "A_NOUVEAU", "OUVERTURE", null, null, null, false,
                Z, Z, maintenant, auteur, auteur, List.of()));

        clotures.substituerLettrages(livreId, exercice.id(), suivant.id());

        periodes.cloturer(livreId, ouverture.id(), auteur);
        periodes.specialePourDate(livreId, "OUVERTURE", exercice.du()).filter(p -> "OUVERTE".equals(p.statut())).ifPresent(p -> periodes.cloturer(livreId, p.id(), auteur));
        periodes.cloturer(livreId, periodeCloture.id(), auteur);

        clotures.clore(livreId, exercice.id(), auteur);
        periodes.ouvrirClotureSiPossible(livreId, suivant.id());

        int extournes = extourner(livreId, exercice);

        List<ControleRepository.EcartOuverture> ecarts = controleRepository.ecartsOuverture(livreId, suivant.id(), exercice.id());
        if (!ecarts.isEmpty()) {
            ControleRepository.EcartOuverture e = ecarts.get(0);
            throw new CommonException("L'ouverture de " + suivant.code() + " diffère de la clôture de " + exercice.code() + " sur le compte " + e.compte() + " : la clôture est annulée.",
                    HttpStatus.INTERNAL_SERVER_ERROR, "COMPTA_OUVERTURE_ECART");
        }
        audit.logAction(ActionAudit.CLOTURE, "compta_exercices", exercice.id(), "Clôture définitive de " + exercice.code());
        return new ClotureDefinitiveDTO("CLOTURE", ecritureCloture, aNouveau.map(a -> a.ecriture().id()).orElse(null), suivant.id(),
                aNouveau.map(ANouveauEcrit::nbLignes).orElse(0L), extournes, null);
    }

    /** Refus si un état recalculé n'est plus celui qui a été figé : on ne clôture pas autre chose que ce qui a été arrêté. */
    private void verifierEtatsFiges(Long livreId, ExerciceDTO exercice) {
        for (String code : EtatService.ETATS_SYNTHESE) {
            EtatFige fige = clotures.dernierFige(livreId, exercice.id(), code).orElseThrow(() -> new CommonException(
                    "L'état " + code + " n'a pas été figé : levez l'arrêté et recommencez.", HttpStatus.CONFLICT, "COMPTA_ETATS_MODIFIES"));
            if (!fige.sha256().equals(etats.empreinte(etats.calculer(livreId, code, exercice, exercice.au())))) {
                throw new CommonException("L'état " + code + " recalculé diffère de sa version figée : levez l'arrêté et recommencez.", HttpStatus.CONFLICT, "COMPTA_ETATS_MODIFIES");
            }
        }
    }

    /** La cascade du CGNC en une seule écriture : 61 et 71 vers 8100, 63 et 73 vers 8300, 65 et 75 vers 8500, 67 vers 8800, puis 8400, 8600, 8800 et 119x. */
    private Long passerEcritureCloture(Long livreId, ExerciceDTO exercice) {
        Map<String, BigDecimal> debits = new HashMap<>();
        Map<String, BigDecimal> credits = new HashMap<>();
        List<NouvelleLigne> lignes = new ArrayList<>();
        for (SoldeGestion s : clotures.soldesGestion(livreId, exercice.id())) {
            String groupe = groupe(s.compte());
            if (s.solde().signum() > 0) {
                lignes.add(ligne(s.compte(), Z, s.solde()));
                debits.merge(groupe, s.solde(), BigDecimal::add);
            } else {
                lignes.add(ligne(s.compte(), s.solde().negate(), Z));
                credits.merge(groupe, s.solde().negate(), BigDecimal::add);
            }
        }
        if (lignes.isEmpty()) {
            return null;
        }
        for (String groupe : List.of("8100", "8300", "8500", "8800")) {
            if (debits.containsKey(groupe)) {
                lignes.add(ligne(groupe, debits.get(groupe), Z));
            }
            if (credits.containsKey(groupe)) {
                lignes.add(ligne(groupe, Z, credits.get(groupe)));
            }
        }
        BigDecimal r81 = net(debits, credits, "8100");
        BigDecimal r83 = net(debits, credits, "8300");
        BigDecimal r85 = net(debits, credits, "8500");
        BigDecimal r88 = net(debits, credits, "8800");
        solder(lignes, "8100", "8400", r81);
        solder(lignes, "8300", "8400", r83);
        BigDecimal r84 = r81.add(r83);
        solder(lignes, "8400", "8600", r84);
        solder(lignes, "8500", "8600", r85);
        BigDecimal r86 = r84.add(r85);
        solder(lignes, "8600", "8800", r86);
        BigDecimal perte = r86.add(r88);
        if (perte.signum() < 0) {
            lignes.add(ligne("8800", perte.negate(), Z));
            lignes.add(ligne("1191", Z, perte.negate()));
        } else if (perte.signum() > 0) {
            lignes.add(ligne("1199", perte, Z));
            lignes.add(ligne("8800", Z, perte));
        }
        JournalDTO journalCloture = journaux.parType(livreId, "CLOTURE").orElseThrow();
        EcritureEnregistree ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(journalCloture.id(), exercice.au(), exercice.au(), "CL-" + exercice.code(),
                "Clôture de l'exercice " + exercice.code(), "CLOTURE", "CLOTURE", null, null, null, false, null, null, lignes));
        return ecriture.id();
    }

    private static String groupe(String compte) {
        if (compte.startsWith("61") || compte.startsWith("71")) {
            return "8100";
        }
        if (compte.startsWith("63") || compte.startsWith("73")) {
            return "8300";
        }
        return compte.startsWith("65") || compte.startsWith("75") ? "8500" : "8800";
    }

    private static BigDecimal net(Map<String, BigDecimal> debits, Map<String, BigDecimal> credits, String groupe) {
        return debits.getOrDefault(groupe, Z).subtract(credits.getOrDefault(groupe, Z));
    }

    /** Solde un compte de résultat dans le suivant. n : son solde débiteur net. */
    private static void solder(List<NouvelleLigne> lignes, String compte, String suivant, BigDecimal n) {
        if (n.signum() > 0) {
            lignes.add(ligne(compte, Z, n));
            lignes.add(ligne(suivant, n, Z));
        } else if (n.signum() < 0) {
            lignes.add(ligne(compte, n.negate(), Z));
            lignes.add(ligne(suivant, Z, n.negate()));
        }
    }

    private static NouvelleLigne ligne(String compte, BigDecimal debit, BigDecimal credit) {
        return new NouvelleLigne(compte, null, debit, credit, null, null, null, false, null, null, null, null, null, null);
    }

    private ExerciceDTO exerciceSuivant(Long livreId, ExerciceDTO exercice, String auteur) {
        List<ExerciceDTO> tous = exercices.lister(livreId);
        Optional<ExerciceDTO> existant = tous.stream().filter(e -> exercice.id().equals(e.precedentId())).findFirst();
        if (existant.isPresent()) {
            return existant.get();
        }
        LocalDate du = exercice.au().plusDays(1);
        LocalDate au = du.plusYears(1).minusDays(1);
        String code = String.valueOf(du.getYear());
        if (tous.stream().anyMatch(e -> e.code().equals(code))) {
            throw new CommonException("Un exercice " + code + " existe déjà : créez l'exercice suivant de " + exercice.code() + " avec un autre code.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_CODE");
        }
        Long id = exerciceService.creer(livreId, new ExerciceDatesRequest(code, du, au), exercice.id(), du, auteur);
        audit.logAction(ActionAudit.CREATION, "compta_exercices", id, "Exercice " + code + " créé à la clôture de " + exercice.code());
        return exiger(livreId, id);
    }

    /** Le miroir de chaque écriture à extourner, dans la première période ouverte de N+1 : un vrai mouvement de N+1. */
    private int extourner(Long livreId, ExerciceDTO exercice) {
        List<Long> liste = clotures.ecrituresAExtourner(livreId, exercice.id());
        for (Long id : liste) {
            EcritureDTO origine = journal.lire(livreId, id).orElseThrow();
            LignePeriode periode = periodes.premiereOuverte(livreId).orElseThrow(() -> new CommonException(
                    "Aucune période ouverte pour passer l'extourne de " + origine.numeroComplet() + ".", HttpStatus.CONFLICT, "COMPTA_PERIODE_INCONNUE"));
            LocalDate date = journal.derniereDate(livreId, periode.exerciceId(), origine.journal().id())
                    .map(d -> d.withDayOfMonth(1)).filter(d -> d.isAfter(periode.du())).orElse(periode.du());
            List<NouvelleLigne> miroir = new ArrayList<>();
            for (LigneJournal l : journal.lignesBrutes(livreId, id)) {
                miroir.add(new NouvelleLigne(l.compte(), l.tiersId(), l.credit(), l.debit(), l.libelle(), l.dateEcheance(), l.natureMouvement(), l.horsFlux(),
                        l.devise(), l.montantDevise(), l.tauxTva(), l.baseTva(), l.id(), l.pointDeVenteId()));
            }
            posteur.poster(livreId, new PosteurEcritures.Proposition(origine.journal().id(), date, origine.datePiece(), origine.refPiece(), "Extourne de " + origine.numeroComplet(),
                    "COURANTE", origine.origine(), id, null, "Extourne à l'ouverture de l'exercice suivant", false, null, null, miroir));
        }
        return liste.size();
    }

    /** Rejouable : ce qui existe déjà n'est pas réécrit. Le FEC se régénère à l'identique, seule son empreinte est gardée ; les états en PDF portent leur date de production, on garde le fichier. */
    public String archiver(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        return archiver(livreId, exerciceId);
    }

    private String archiver(Long livreId, Long exerciceId) {
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        if (!"CLOTURE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " n'est pas clôturé.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_CLOS");
        }
        String auteur = livreCourant.auteur();
        Optional<String> dejaFec = clotures.empreinteArchive(livreId, exerciceId, ARCHIVE_FEC);
        String sha = dejaFec.orElseGet(() -> empreinteFec(livreId, exerciceId));
        byte[] pdf = clotures.empreinteArchive(livreId, exerciceId, ARCHIVE_ETATS).isPresent() ? null
                : exports.pdf(EtatService.ETATS_SYNTHESE.stream().map(code -> etats.etat(exerciceId, code, null)).toList());
        transaction.executeWithoutResult(statut -> {
            if (dejaFec.isEmpty()) {
                clotures.archiver(livreId, exerciceId, ARCHIVE_FEC, "FEC_" + exercice.code() + ".txt", sha, clotures.nbLignes(livreId, exerciceId), null, auteur);
            }
            if (pdf != null) {
                clotures.archiver(livreId, exerciceId, ARCHIVE_ETATS, "ETATS_" + exercice.code() + ".pdf", empreinte(pdf), null, pdf, auteur);
            }
        });
        return sha;
    }

    /** L'empreinte du FEC tel que le service le produit aujourd'hui, options par défaut. */
    public String empreinteFec(Long livreId, Long exerciceId) {
        MessageDigest empreinte = sha256();
        fec.ecrire(new FecService.ExportFec(livreId, exerciceId, "", FecService.OptionsFec.parDefaut()), new DigestOutputStream(OutputStream.nullOutputStream(), empreinte));
        return HexFormat.of().formatHex(empreinte.digest());
    }

    private static String empreinte(byte[] contenu) {
        return HexFormat.of().formatHex(sha256().digest(contenu));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * En N+1, après l'assemblée. Bénéfice : D 1191 / C réserves, report, dividendes. Perte : D 1169 ou 1189 / C 1199.
     * Tout le résultat s'affecte d'un coup : la part non décidée va sur 1181 ou 1189, comme le dit le plan.
     */
    @Transactional
    public EcritureDTO affecter(Long exerciceId, AffectationRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exiger(livreId, exerciceId);
        if (!"CLOTURE".equals(exercice.statut())) {
            throw new CommonException("L'exercice " + exercice.code() + " n'est pas clôturé : son résultat ne s'affecte qu'après.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_NON_CLOS");
        }
        BigDecimal reste = resteAAffecter(livreId, exercice);
        if (reste.signum() == 0) {
            throw new CommonException("Le résultat de " + exercice.code() + " est déjà affecté.", HttpStatus.CONFLICT, "COMPTA_AFFECTATION_FAITE");
        }
        boolean benefice = reste.signum() > 0;
        List<String> admis = benefice ? COMPTES_BENEFICE : COMPTES_PERTE;
        BigDecimal total = Z;
        List<NouvelleLigne> lignes = new ArrayList<>();
        for (AffectationLigneRequest l : demande.lignes()) {
            if (l.montant().signum() <= 0) {
                throw new CommonException("Compte " + l.compte() + " : le montant doit être positif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_AFFECTATION_INVALIDE");
            }
            if (admis.stream().noneMatch(l.compte()::startsWith)) {
                throw new CommonException("Le compte " + l.compte() + " ne reçoit pas " + (benefice ? "un bénéfice" : "une perte") + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_AFFECTATION_INVALIDE");
            }
            total = total.add(l.montant());
            boolean horsFlux = !l.compte().startsWith("4465");
            lignes.add(new NouvelleLigne(l.compte(), null, benefice ? Z : l.montant(), benefice ? l.montant() : Z, null, null, "AFFECTATION", horsFlux, null, null, null, null, null, null));
        }
        if (total.compareTo(reste.abs()) != 0) {
            throw new CommonException("Le résultat à affecter est de " + MontantTexte.fr(reste.abs()) + ", les lignes font " + MontantTexte.fr(total) + ".",
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_AFFECTATION_ECART");
        }
        lignes.add(0, new NouvelleLigne(benefice ? "1191" : "1199", null, benefice ? total : Z, benefice ? Z : total, null, null, "AFFECTATION", true, null, null, null, null, null, null));
        JournalDTO od = journaux.parType(livreId, "OD").orElseThrow();
        EcritureEnregistree ecriture = posteur.poster(livreId, new PosteurEcritures.Proposition(od.id(), demande.dateDecision(), demande.dateDecision(), "AFFECTATION-" + exercice.code(),
                "Affectation du résultat " + exercice.code(), "COURANTE", "AFFECTATION", null, null, null, false, null, null, lignes));
        audit.logAction(ActionAudit.MODIFICATION, "compta_exercices", exerciceId, "Résultat " + exercice.code() + " affecté par l'écriture " + ecriture.id());
        return journal.lire(livreId, ecriture.id()).orElseThrow();
    }

    /** Ce qui reste sur 1191 et 1199 dans l'exercice suivant : positif, un bénéfice ; négatif, une perte. */
    private BigDecimal resteAAffecter(Long livreId, ExerciceDTO exercice) {
        Optional<ExerciceDTO> suivant = exercices.lister(livreId).stream().filter(e -> exercice.id().equals(e.precedentId())).findFirst();
        if (suivant.isEmpty()) {
            return Z;
        }
        ExerciceDTO s = suivant.get();
        BigDecimal reste = Z;
        for (MesuresRepository.Mesure m : mesures.parCompte(livreId, s.id(), mesures.chaineNonClose(livreId, s.id()), s.au(), false)) {
            if (m.compte().startsWith("1191") || m.compte().startsWith("1199")) {
                reste = reste.add(m.sc()).subtract(m.sd());
            }
        }
        return reste;
    }

    private ExerciceDTO exiger(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }

    private static String tronquer(String texte) {
        return texte.length() > 900 ? texte.substring(0, 900) : texte;
    }
}
