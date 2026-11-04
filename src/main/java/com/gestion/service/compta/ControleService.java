package com.gestion.service.compta;

import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EtatDtos.RapportControlesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ResultatControleDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.repository.compta.ClotureRepository;
import com.gestion.repository.compta.ConsultationRepository;
import com.gestion.repository.compta.ControleRepository;
import com.gestion.repository.compta.ControleRepository.Numerotation;
import com.gestion.repository.compta.ControleRepository.ResultatEcrit;
import com.gestion.repository.compta.ControleRepository.TotauxJournal;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.service.compta.DefinitionsEtats.Controle;
import com.gestion.service.compta.MoteurEtats.JeuMesures;
import com.gestion.service.compta.MoteurEtats.MesureCompte;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Rejoue les contrôles et écrit ce qui a tourné. Ce qui n'a pas tourné n'a pas de ligne et s'affiche « non effectué », jamais réussi. */
@Service
public class ControleService {

    private static final String BLOQUANT = "BLOQUANT";
    private static final String BLOQUE_CLOTURE = "BLOQUE_CLOTURE";
    private static final String AVERTISSEMENT = "AVERTISSEMENT";
    private static final Pattern COMPTE_EN_TETE = Pattern.compile("^(\\d{2,12})\\s");

    /** Tous les contrôles connus : la liste affichée ne dépend pas de ce qui a pu tourner. */
    private static final Map<String, String> CONNUS;

    static {
        Map<String, String> connus = new LinkedHashMap<>();
        connus.put("EQUILIBRE_ECRITURE", BLOQUANT);
        connus.put("EQUILIBRE_JOURNAL", BLOQUANT);
        connus.put("JOURNAL_EGAL_GRAND_LIVRE", BLOQUANT);
        connus.put("BALANCE_SOLDES", BLOQUANT);
        connus.put("AUXILIAIRE_EGAL_COLLECTIF", BLOQUANT);
        connus.put("NUMEROTATION", BLOQUANT);
        for (Controle c : DefinitionsEtats.instance().controles()) {
            connus.putIfAbsent(c.code(), gravite(c.gravite()));
        }
        connus.put("CHRONO_JOURNAL", AVERTISSEMENT);
        connus.put("ARCHIVE_ABSENTE", AVERTISSEMENT);
        CONNUS = Map.copyOf(connus);
        ORDRE = List.copyOf(connus.keySet());
    }

    private static final List<String> ORDRE;

    private final ControleRepository repository;
    private final ConsultationRepository consultation;
    private final ExerciceRepository exercices;
    private final EtatService etats;
    private final LivreCourantService livreCourant;
    private final ClotureRepository clotures;
    private final DefinitionsEtats definitions = DefinitionsEtats.instance();

    public ControleService(ControleRepository repository, ConsultationRepository consultation, ExerciceRepository exercices, EtatService etats, LivreCourantService livreCourant,
                           ClotureRepository clotures) {
        this.clotures = clotures;
        this.repository = repository;
        this.consultation = consultation;
        this.exercices = exercices;
        this.etats = etats;
        this.livreCourant = livreCourant;
    }

    /** SENS_ANORMAL n'a pas une gravité mais deux, selon la classe : il l'indique sur chaque résultat. */
    static String gravite(String definition) {
        if (definition.startsWith("bloque_cloture")) {
            return BLOQUE_CLOTURE;
        }
        return definition.startsWith("bloquant") ? BLOQUANT : AVERTISSEMENT;
    }

    @Transactional(readOnly = true)
    public RapportControlesDTO dernier(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        exigerExercice(livreId, exerciceId);
        return rapport(livreId, exerciceId);
    }

    @Transactional
    public RapportControlesDTO rejouer(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        Execution execution = new Execution(livreId, exerciceId, UUID.randomUUID(), livreCourant.auteur());

        controlesDuJournal(execution);
        List<JeuMesures> pile = etats.pile(livreId, exercice, exercice.au());
        MoteurEtats moteur = new MoteurEtats(definitions, pile);
        controlesDeLaDefinition(execution, moteur);
        JeuMesures jeu = pile.get(0);
        balanceSoldes(execution, jeu);
        compteNonAffecte(execution, jeu);
        classe8(execution, jeu);
        comptesASolder(execution, jeu);
        sensAnormal(execution, jeu);
        ligneNegative(execution, moteur);
        resultatNonAffecte(execution, jeu);
        ouverture(execution, exercice);
        archive(execution, exercice);
        return rapport(livreId, exerciceId);
    }

    private void controlesDuJournal(Execution x) {
        long desequilibrees = repository.ecrituresDesequilibrees(x.livreId(), x.exerciceId());
        x.ecrire("EQUILIBRE_ECRITURE", BLOQUANT, desequilibrees == 0, null, null, desequilibrees == 0 ? null : desequilibrees + " écriture(s) déséquilibrée(s).");

        List<TotauxJournal> totaux = repository.totauxParJournal(x.livreId(), x.exerciceId());
        boolean journauxBons = true;
        for (TotauxJournal t : totaux) {
            if (t.debit().compareTo(t.credit()) != 0) {
                journauxBons = false;
                x.ecrire("EQUILIBRE_JOURNAL", BLOQUANT, false, t.debit(), t.credit(), "Journal " + t.code() + " déséquilibré.");
            }
        }
        if (journauxBons) {
            x.ecrire("EQUILIBRE_JOURNAL", BLOQUANT, true, null, null, null);
        }

        BigDecimal[] comparaison = repository.journalContreGrandLivre(x.livreId(), x.exerciceId());
        boolean egaux = comparaison[0].compareTo(comparaison[1]) == 0;
        x.ecrire("JOURNAL_EGAL_GRAND_LIVRE", BLOQUANT, egaux, comparaison[0], comparaison[1], egaux ? null : "Le journal et le grand livre diffèrent.");

        long orphelines = repository.lignesSansTiersSurCollectif(x.livreId(), x.exerciceId());
        x.ecrire("AUXILIAIRE_EGAL_COLLECTIF", BLOQUANT, orphelines == 0, null, null, orphelines == 0 ? null : orphelines + " ligne(s) sans tiers sur un compte collectif.");

        boolean numerosBons = true;
        for (Numerotation n : repository.numerotation(x.livreId(), x.exerciceId())) {
            if (n.minimum() != 1 || n.maximum() != n.nombre()) {
                numerosBons = false;
                x.ecrire("NUMEROTATION", BLOQUANT, false, BigDecimal.valueOf(n.maximum()), BigDecimal.valueOf(n.nombre()), "Journal " + n.code() + " : rupture de numérotation.");
            }
        }
        if (numerosBons) {
            x.ecrire("NUMEROTATION", BLOQUANT, true, null, null, null);
        }

        long hors = repository.ruptureChronologie(x.livreId(), x.exerciceId());
        x.ecrire("CHRONO_JOURNAL", AVERTISSEMENT, hors == 0, null, null, hors == 0 ? null : hors + " pièce(s) datée(s) avant la pièce précédente du journal.");
    }

    private void controlesDeLaDefinition(Execution x, MoteurEtats moteur) {
        for (Controle c : definitions.controles()) {
            if (c.gauche() == null) {
                continue;
            }
            BigDecimal gauche = moteur.expression(c.gauche());
            BigDecimal droite = c.droiteTermes() != null ? moteur.termes(c.droiteTermes()) : moteur.expression(c.droite());
            if (gauche == null || droite == null) {
                continue;
            }
            BigDecimal a = deux(gauche);
            BigDecimal b = deux(droite);
            boolean reussi = a.compareTo(b) == 0;
            x.ecrire(c.code(), gravite(c.gravite()), reussi, a, b, reussi ? null : "Écart de " + MontantTexte.fr(a.subtract(b)) + ".");
        }
    }

    private static void balanceSoldes(Execution x, JeuMesures jeu) {
        BigDecimal sd = BigDecimal.ZERO;
        BigDecimal sc = BigDecimal.ZERO;
        for (MesureCompte m : jeu.mesures().values()) {
            sd = sd.add(m.sd());
            sc = sc.add(m.sc());
        }
        boolean reussi = sd.compareTo(sc) == 0;
        x.ecrire("BALANCE_SOLDES", BLOQUANT, reussi, sd, sc, reussi ? null : "La balance ne s'équilibre pas.");
    }

    private void compteNonAffecte(Execution x, JeuMesures jeu) {
        boolean tous = true;
        for (Map.Entry<String, MesureCompte> e : jeu.mesures().entrySet()) {
            String compte = e.getKey();
            if (compte.charAt(0) < '1' || compte.charAt(0) > '7') {
                continue;
            }
            MesureCompte m = e.getValue();
            if ((m.sd().signum() != 0 && definitions.regle(compte, 'D') == null) || (m.sc().signum() != 0 && definitions.regle(compte, 'C') == null)) {
                tous = false;
                x.ecrire("COMPTE_NON_AFFECTE", BLOQUANT, false, m.sd(), m.sc(), compte + " n'appartient à aucune ligne d'état.");
            }
        }
        if (tous) {
            x.ecrire("COMPTE_NON_AFFECTE", BLOQUANT, true, null, null, null);
        }
    }

    private static void classe8(Execution x, JeuMesures jeu) {
        boolean bon = true;
        for (Map.Entry<String, MesureCompte> e : jeu.mesures().entrySet()) {
            MesureCompte m = e.getValue();
            if (e.getKey().startsWith("8") && (m.sd().signum() != 0 || m.sc().signum() != 0)) {
                bon = false;
                x.ecrire("CLASSE_8_NON_NULLE", BLOQUANT, false, m.sd(), m.sc(), e.getKey() + " a un solde hors écritures de clôture.");
            }
        }
        if (bon) {
            x.ecrire("CLASSE_8_NON_NULLE", BLOQUANT, true, null, null, null);
        }
    }

    private void comptesASolder(Execution x, JeuMesures jeu) {
        Map<String, String> libelles = consultation.libellesComptes(x.livreId());
        boolean bon = true;
        for (Map.Entry<String, MesureCompte> e : jeu.mesures().entrySet()) {
            String compte = e.getKey();
            if (definitions.comptesASolder().stream().noneMatch(compte::startsWith)) {
                continue;
            }
            BigDecimal solde = e.getValue().sd().add(e.getValue().sc());
            if (solde.signum() != 0) {
                bon = false;
                x.ecrire("COMPTES_A_SOLDER", BLOQUE_CLOTURE, false, solde, BigDecimal.ZERO,
                        compte + " " + libelles.getOrDefault(compte, "") + " n'est pas soldé : " + MontantTexte.fr(solde) + ".");
            }
        }
        if (bon) {
            x.ecrire("COMPTES_A_SOLDER", BLOQUE_CLOTURE, true, null, null, null);
        }
    }

    private void sensAnormal(Execution x, JeuMesures jeu) {
        Map<String, String> libelles = consultation.libellesComptes(x.livreId());
        boolean bilan = true;
        boolean gestion = true;
        for (Map.Entry<String, MesureCompte> e : jeu.mesures().entrySet()) {
            String compte = e.getKey();
            char classe = compte.charAt(0);
            if (classe < '1' || classe > '7') {
                continue;
            }
            MesureCompte m = e.getValue();
            boolean debit = EtatService.estAnormal(compte, 'D', m.sd(), definitions);
            boolean credit = EtatService.estAnormal(compte, 'C', m.sc(), definitions);
            if (debit || credit) {
                boolean deBilan = classe <= '5';
                bilan &= !deBilan;
                gestion &= deBilan;
                BigDecimal montant = debit ? m.sd() : m.sc();
                x.ecrire("SENS_ANORMAL", deBilan ? BLOQUE_CLOTURE : AVERTISSEMENT, false, debit ? m.sd() : null, credit ? m.sc() : null,
                        compte + " " + libelles.getOrDefault(compte, "") + " : solde " + (debit ? "débiteur" : "créditeur") + " de " + MontantTexte.fr(montant) + ", contraire à son sens.");
            }
        }
        if (bilan) {
            x.ecrire("SENS_ANORMAL", BLOQUE_CLOTURE, true, null, null, null);
        }
        if (gestion) {
            x.ecrire("SENS_ANORMAL", AVERTISSEMENT, true, null, null, null);
        }
    }

    private void ligneNegative(Execution x, MoteurEtats moteur) {
        boolean bon = true;
        for (String code : List.of("BILAN_ACTIF", "BILAN_PASSIF", "CPC")) {
            DefinitionsEtats.Etat etat = definitions.etat(code);
            for (DefinitionsEtats.Ligne ligne : etat.lignes()) {
                if (!Boolean.FALSE.equals(ligne.negatifAutorise())) {
                    continue;
                }
                for (DefinitionsEtats.Colonne colonne : etat.colonnes()) {
                    BigDecimal valeur = colonne.precedent() ? null : moteur.cellule(ligne.code(), colonne.code());
                    if (valeur != null && valeur.signum() < 0) {
                        bon = false;
                        x.ecrire("LIGNE_NEGATIVE", BLOQUE_CLOTURE, false, deux(valeur), BigDecimal.ZERO,
                                ligne.libelle() + " (" + colonne.libelle() + ") est négatif : " + MontantTexte.fr(valeur) + ".");
                    }
                }
            }
        }
        if (bon) {
            x.ecrire("LIGNE_NEGATIVE", BLOQUE_CLOTURE, true, null, null, null);
        }
    }

    private static void resultatNonAffecte(Execution x, JeuMesures jeu) {
        BigDecimal solde = BigDecimal.ZERO;
        for (Map.Entry<String, MesureCompte> e : jeu.mesures().entrySet()) {
            if (e.getKey().startsWith("1191") || e.getKey().startsWith("1199")) {
                solde = solde.add(e.getValue().sd()).add(e.getValue().sc());
            }
        }
        boolean bon = solde.signum() == 0;
        x.ecrire("RESULTAT_N1_NON_AFFECTE", AVERTISSEMENT, bon, solde, BigDecimal.ZERO, bon ? null : "Le résultat de l'exercice précédent attend son affectation : " + MontantTexte.fr(solde) + ".");
    }

    /** Sans exercice précédent clos, il n'y a pas d'ouverture à comparer : le contrôle ne tourne pas. */
    private void ouverture(Execution x, ExerciceDTO exercice) {
        if (exercice.precedentId() == null) {
            return;
        }
        Optional<ExerciceDTO> precedent = exercices.lire(x.livreId(), exercice.precedentId());
        if (precedent.isEmpty() || !"CLOTURE".equals(precedent.get().statut())) {
            return;
        }
        List<ControleRepository.EcartOuverture> ecarts = repository.ecartsOuverture(x.livreId(), exercice.id(), exercice.precedentId());
        ecarts.forEach(e -> x.ecrire("AN_EGAL_CLOTURE_N1", BLOQUANT, false, e.ouverture(), e.cloture(), e.compte() + " : l'ouverture diffère de la clôture de l'exercice précédent."));
        if (ecarts.isEmpty()) {
            x.ecrire("AN_EGAL_CLOTURE_N1", BLOQUANT, true, null, null, null);
        }
    }

    /** Un exercice clos sans archive se signale : elle se rejoue depuis l'écran de clôture. */
    private void archive(Execution x, ExerciceDTO exercice) {
        if ("CLOTURE".equals(exercice.statut())) {
            boolean presente = clotures.empreinteArchive(x.livreId(), exercice.id(), "FEC").isPresent();
            x.ecrire("ARCHIVE_ABSENTE", AVERTISSEMENT, presente, null, null, presente ? null : "L'archive de clôture manque : rejouez-la depuis l'écran de clôture.");
        }
    }

    private RapportControlesDTO rapport(Long livreId, Long exerciceId) {
        Optional<UUID> execution = repository.derniereExecution(livreId, exerciceId);
        List<ResultatEcrit> ecrits = execution.map(id -> repository.resultats(livreId, exerciceId, id)).orElse(List.of());
        List<ResultatControleDTO> resultats = new ArrayList<>();
        for (String code : ORDRE) {
            List<ResultatEcrit> duCode = ecrits.stream().filter(r -> r.code().equals(code)).toList();
            if (duCode.isEmpty()) {
                resultats.add(new ResultatControleDTO(code, CONNUS.get(code), null, null, null, raisonNonEffectue(code, execution.isPresent()), null));
            }
            duCode.forEach(r -> resultats.add(new ResultatControleDTO(r.code(), r.gravite(), r.reussi(), deuxOuNul(r.valeurA()), deuxOuNul(r.valeurB()), r.message(), lien(r.message()))));
        }
        long bloquants = ecrits.stream().filter(r -> !r.reussi() && BLOQUANT.equals(r.gravite())).count();
        LocalDateTime le = ecrits.isEmpty() ? null : ecrits.get(0).executeLe();
        String par = ecrits.isEmpty() ? null : ecrits.get(0).executePar();
        return new RapportControlesDTO(execution.map(UUID::toString).orElse(null), le, par, bloquants, resultats);
    }

    private static String raisonNonEffectue(String code, boolean rejoue) {
        if (!rejoue) {
            return "Les contrôles n'ont pas encore été rejoués.";
        }
        if ("ARCHIVE_ABSENTE".equals(code)) {
            return "L'exercice n'est pas clôturé : pas encore d'archive.";
        }
        if ("AN_EGAL_CLOTURE_N1".equals(code)) {
            return "Exercice précédent absent ou non clos : pas d'ouverture à comparer.";
        }
        return "Pas d'exercice précédent ni de balance reprise : une des deux valeurs n'existe pas.";
    }

    private static String lien(String message) {
        if (message == null) {
            return null;
        }
        Matcher m = COMPTE_EN_TETE.matcher(message);
        return m.find() ? "/comptabilite/grand-livre?compte=" + m.group(1) : null;
    }

    private ExerciceDTO exigerExercice(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }

    private static BigDecimal deuxOuNul(BigDecimal valeur) {
        return valeur == null ? null : deux(valeur);
    }

    private static BigDecimal deux(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }

    private final class Execution {
        private final Long livreId;
        private final Long exerciceId;
        private final UUID id;
        private final String auteur;

        private Execution(Long livreId, Long exerciceId, UUID id, String auteur) {
            this.livreId = livreId;
            this.exerciceId = exerciceId;
            this.id = id;
            this.auteur = auteur;
        }

        private Long livreId() {
            return livreId;
        }

        private Long exerciceId() {
            return exerciceId;
        }

        private void ecrire(String code, String gravite, boolean reussi, BigDecimal a, BigDecimal b, String message) {
            repository.ecrire(livreId, exerciceId, id, code, gravite, reussi, a, b, message, auteur);
        }
    }
}
