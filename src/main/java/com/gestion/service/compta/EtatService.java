package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.gestion.persistent.dto.compta.EtatDtos.ColonneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.CompteCelluleDTO;
import com.gestion.persistent.dto.compta.EtatDtos.ControleEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.DetailCelluleDTO;
import com.gestion.persistent.dto.compta.EtatDtos.EtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.LigneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.MentionsEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseLigneDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseSoldesDTO;
import com.gestion.persistent.dto.compta.EtatDtos.RepriseSoldesRequest;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.repository.compta.BrouillonRepository;
import com.gestion.repository.compta.ClotureRepository;
import com.gestion.repository.compta.ClotureRepository.EtatFige;
import com.gestion.repository.compta.ConsultationRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.MesuresRepository;
import com.gestion.repository.compta.RepriseRepository;
import com.gestion.service.compta.DefinitionsEtats.Controle;
import com.gestion.service.compta.DefinitionsEtats.Etat;
import com.gestion.service.compta.DefinitionsEtats.Ligne;
import com.gestion.service.compta.DefinitionsEtats.Regle;
import com.gestion.service.compta.MoteurEtats.JeuMesures;
import com.gestion.service.compta.MoteurEtats.Marquee;
import com.gestion.service.compta.MoteurEtats.MesureCompte;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Un seul résultat pour l'écran, le PDF et l'Excel : aucun support ne calcule de son côté. */
@Service
public class EtatService {

    public static final List<String> ETATS_SYNTHESE = List.of("BILAN_ACTIF", "BILAN_PASSIF", "CPC", "ESG_TFR", "ESG_CAF", "TF_MASSES", "TF_EMPLOIS_RESSOURCES");
    /** Ce qui se lit et s'imprime ensemble : un seul fichier, pas deux téléchargements. */
    private static final Map<String, List<String>> GROUPES = Map.of("BILAN", List.of("BILAN_ACTIF", "BILAN_PASSIF"), "ESG", List.of("ESG_TFR", "ESG_CAF"),
            "TF", List.of("TF_MASSES", "TF_EMPLOIS_RESSOURCES"));
    private static final Map<String, Integer> NIVEAUX = Map.of("total", 0, "resultat", 0, "rubrique", 1, "rubrique_poste", 1, "solde", 1, "detail", 3);
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Pattern REFERENCE = Pattern.compile("([A-Za-z_][A-Za-z0-9_.]*)\\[[A-Z0-9_]+]");
    private static final String LOGICIEL = "Tadbeer comptabilité 1.4.0";

    private final ExerciceRepository exercices;
    private final MesuresRepository mesures;
    private final RepriseRepository reprises;
    private final BrouillonRepository brouillons;
    private final ConsultationRepository consultation;
    private final LivreCourantService livreCourant;
    private final LivreService livres;
    private final EtatExports exports;
    private final ClotureRepository clotures;
    private final ObjectMapper json;
    private final DefinitionsEtats definitions = DefinitionsEtats.instance();

    public EtatService(ExerciceRepository exercices, MesuresRepository mesures, RepriseRepository reprises, BrouillonRepository brouillons,
                       ConsultationRepository consultation, LivreCourantService livreCourant, LivreService livres, EtatExports exports,
                       ClotureRepository clotures, ObjectMapper json) {
        this.clotures = clotures;
        this.json = json;
        this.exercices = exercices;
        this.mesures = mesures;
        this.reprises = reprises;
        this.brouillons = brouillons;
        this.consultation = consultation;
        this.livreCourant = livreCourant;
        this.livres = livres;
        this.exports = exports;
    }

    @Transactional(readOnly = true)
    public EtatDTO etat(Long exerciceId, String code, LocalDate arrete) {
        Long livreId = livreCourant.exiger().livreId();
        exigerEtat(code);
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        LocalDate date = arrete != null ? arrete : exercice.au();
        // Un exercice arrêté ou clos relit ce qui a été figé, jamais un recalcul. Une autre date d'arrêté reste une consultation.
        if (!"OUVERT".equals(exercice.statut()) && date.equals(exercice.au())) {
            Optional<EtatFige> fige = clotures.dernierFige(livreId, exerciceId, code);
            if (fige.isPresent()) {
                return depuisFige(fige.get());
            }
        }
        return calculer(livreId, code, exercice, date);
    }

    /** Le calcul lui-même : l'arrêté le fige, la clôture définitive le refait pour comparer. */
    EtatDTO calculer(Long livreId, String code, ExerciceDTO exercice, LocalDate date) {
        Etat etat = exigerEtat(code);
        Long exerciceId = exercice.id();
        List<JeuMesures> jeux = pile(livreId, exercice, date);
        MoteurEtats moteur = new MoteurEtats(definitions, jeux);

        Map<String, List<String>> alertes = alertesParLigne(etat, jeux.get(0));
        List<LigneEtatDTO> lignes = new ArrayList<>();
        for (Ligne ligne : etat.lignes()) {
            if ("ligne_libre".equals(ligne.type())) {
                continue;
            }
            Map<String, String> valeurs = new LinkedHashMap<>();
            List<String> alertesLigne = new ArrayList<>(alertes.getOrDefault(ligne.code(), List.of()));
            for (DefinitionsEtats.Colonne colonne : etat.colonnes()) {
                BigDecimal valeur = moteur.cellule(ligne.code(), colonne.code());
                valeurs.put(colonne.code(), valeur == null ? null : deux(valeur).toPlainString());
                if (!colonne.precedent() && valeur != null && valeur.signum() < 0 && Boolean.FALSE.equals(ligne.negatifAutorise())
                        && !alertesLigne.contains("LIGNE_NEGATIVE")) {
                    alertesLigne.add("LIGNE_NEGATIVE");
                }
            }
            lignes.add(new LigneEtatDTO(ligne.code(), ligne.libelle(), ligne.type(), NIVEAUX.getOrDefault(ligne.type(), 2), ligne.masse(), ligne.horsTotal(), valeurs, alertesLigne));
        }

        List<ColonneEtatDTO> colonnes = etat.colonnes().stream().map(c -> new ColonneEtatDTO(c.code(), c.libelle())).toList();
        String periode = DATE_FR.format(exercice.du()) + " au " + DATE_FR.format(date);
        MentionsEtatDTO mentions = new MentionsEtatDTO(livres.etat().raisonSociale(), etat.titre(), exercice.code(), periode, LocalDateTime.now(), LOGICIEL);
        boolean provisoire = !mesures.chaineNonClose(livreId, exerciceId).isEmpty();
        long enCours = brouillons.compter(livreId, exercice.du(), date);
        return new EtatDTO(etat.code(), etat.titre(), definitions.version(), "CALCUL", null, provisoire, enCours, date, mentions, colonnes, lignes,
                controles(etat, moteur));
    }

    private EtatDTO depuisFige(EtatFige fige) {
        try {
            EtatDTO e = json.readValue(fige.contenu(), EtatDTO.class);
            MentionsEtatDTO m = e.mentions();
            return new EtatDTO(e.code(), e.titre(), e.versionDefinition(), "FIGE", fige.figeage(), e.ouvertureProvisoire(), 0, e.arrete(),
                    new MentionsEtatDTO(m.entite(), m.typeEtat(), m.exercice(), m.periode(), LocalDateTime.now(), m.logiciel()), e.colonnes(), e.lignes(), e.controles());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("État figé illisible", ex);
        }
    }

    String contenu(EtatDTO etat) {
        try {
            return json.writeValueAsString(etat);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("État non sérialisable : " + etat.code(), e);
        }
    }

    /** Sur les valeurs seules : ni la date de production, ni les mentions ne changent ce que dit l'état. */
    String empreinte(EtatDTO etat) {
        StringBuilder texte = new StringBuilder(etat.versionDefinition()).append('\n');
        for (LigneEtatDTO ligne : etat.lignes()) {
            texte.append(ligne.code());
            for (ColonneEtatDTO colonne : etat.colonnes()) {
                texte.append('|').append(colonne.code()).append('=').append(ligne.valeurs().get(colonne.code()));
            }
            texte.append('\n');
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texte.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Le résultat de l'exercice tel que le CPC le donne : le bilan et l'arrêté lisent la même cellule. */
    BigDecimal resultatNet(Long livreId, ExerciceDTO exercice) {
        BigDecimal resultat = new MoteurEtats(definitions, pile(livreId, exercice, exercice.au())).cellule("CPC.XVI", "TOTAUX");
        return resultat == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : deux(resultat);
    }

    @Transactional(readOnly = true)
    public DetailCelluleDTO detail(Long exerciceId, String code, String ligne, String colonne, LocalDate arrete) {
        Long livreId = livreCourant.exiger().livreId();
        Etat etat = exigerEtat(code);
        if (etat.lignes().stream().noneMatch(l -> l.code().equals(ligne)) || etat.colonnes().stream().noneMatch(c -> c.code().equals(colonne))) {
            throw new ResourceNotFoundException("Cellule d'état", "ligne", ligne + "/" + colonne);
        }
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        MoteurEtats moteur = new MoteurEtats(definitions, pile(livreId, exercice, arrete != null ? arrete : exercice.au()));
        Map<String, String> libelles = consultation.libellesComptes(livreId);
        List<CompteCelluleDTO> comptes = moteur.contributions(ligne, colonne).stream()
                .map(c -> new CompteCelluleDTO(c.compte(), libelles.getOrDefault(c.compte(), c.compte()), c.mesure(), deux(c.montant()), c.reclassement())).toList();
        return new DetailCelluleDTO(moteur.formule(ligne, colonne), comptes);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long exerciceId, String code, LocalDate arrete) {
        return exports.pdf(groupe(exerciceId, code, arrete));
    }

    @Transactional(readOnly = true)
    public byte[] xlsx(Long exerciceId, String code, LocalDate arrete) {
        return exports.xlsx(groupe(exerciceId, code, arrete));
    }

    private List<EtatDTO> groupe(Long exerciceId, String code, LocalDate arrete) {
        return GROUPES.getOrDefault(code, List.of(code)).stream().map(c -> etat(exerciceId, c, arrete)).toList();
    }

    @Transactional(readOnly = true)
    public RepriseSoldesDTO reprise(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        LocalDate fin = exercice.du().minusDays(1);
        List<RepriseLigneDTO> lignes = reprises.lignes(livreId, fin);
        return new RepriseSoldesDTO(exercice.du().minusYears(1), fin, exercice.precedentId() == null, total(lignes, true), total(lignes, false), lignes);
    }

    /** Une balance déséquilibrée donnerait un bilan N-1 faux sans que rien ne le dise. */
    @Transactional
    public RepriseSoldesDTO enregistrerReprise(Long exerciceId, RepriseSoldesRequest demande) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO exercice = exigerExercice(livreId, exerciceId);
        if (exercice.precedentId() != null) {
            throw new CommonException("L'exercice précédent est dans l'application : sa balance vient de ses écritures.", HttpStatus.CONFLICT, "COMPTA_REPRISE_INUTILE");
        }
        for (RepriseLigneDTO ligne : demande.lignes()) {
            if (ligne.soldeDebit().signum() < 0 || ligne.soldeCredit().signum() < 0) {
                throw new CommonException("Compte " + ligne.compte() + " : un solde repris ne peut pas être négatif.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_REPRISE_INVALIDE");
            }
        }
        BigDecimal debit = total(demande.lignes(), true);
        BigDecimal credit = total(demande.lignes(), false);
        if (debit.compareTo(credit) != 0) {
            throw new CommonException("Balance déséquilibrée : débit " + MontantTexte.fr(debit) + ", crédit " + MontantTexte.fr(credit) + ".",
                    HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_REPRISE_DESEQUILIBREE");
        }
        LocalDate fin = exercice.du().minusDays(1);
        reprises.remplacer(livreId, exercice.du().minusYears(1), fin, demande.lignes(), livreCourant.auteur());
        return reprise(exerciceId);
    }

    /** N, puis N-1 et N-2. Un exercice absent de l'application se lit dans sa balance reprise ; sans elle, l'élément reste nul. */
    List<JeuMesures> pile(Long livreId, ExerciceDTO exercice, LocalDate arrete) {
        List<JeuMesures> pile = new ArrayList<>();
        pile.add(jeu(livreId, exercice, arrete));
        ExerciceDTO courant = exercice;
        LocalDate debut = exercice.du();
        for (int i = 0; i < 2; i++) {
            if (courant != null && courant.precedentId() != null) {
                courant = exigerExercice(livreId, courant.precedentId());
                pile.add(jeu(livreId, courant, courant.au()));
                debut = courant.du();
            } else {
                courant = null;
                pile.add(reprise(livreId, debut.minusDays(1)));
                debut = debut.minusYears(1);
            }
        }
        return pile;
    }

    private JeuMesures jeu(Long livreId, ExerciceDTO exercice, LocalDate arrete) {
        List<Long> chaine = mesures.chaineNonClose(livreId, exercice.id());
        Map<String, MesureCompte> parCompte = new TreeMap<>();
        for (MesuresRepository.Mesure m : mesures.parCompte(livreId, exercice.id(), chaine, arrete, true)) {
            parCompte.put(m.compte(), new MesureCompte(m.od(), m.oc(), m.md(), m.mc(), m.sd(), m.sc()));
        }
        List<Marquee> marquees = mesures.marquees(livreId, exercice.id(), arrete).stream()
                .map(l -> new Marquee(l.compte(), l.nature(), l.horsFlux(), l.md(), l.mc())).toList();
        return new JeuMesures(parCompte, marquees);
    }

    private JeuMesures reprise(Long livreId, LocalDate dateFin) {
        List<RepriseLigneDTO> lignes = reprises.lignes(livreId, dateFin);
        if (lignes.isEmpty()) {
            return null;
        }
        Map<String, MesureCompte> parCompte = new TreeMap<>();
        for (RepriseLigneDTO l : lignes) {
            parCompte.put(l.compte(), new MesureCompte(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, l.soldeDebit(), l.soldeCredit()));
        }
        return new JeuMesures(parCompte, List.of());
    }

    /** Sens contraire au sens naturel, hors reclassement prévu : le compte reste sur sa ligne et l'alerte le montre. */
    private Map<String, List<String>> alertesParLigne(Etat etat, JeuMesures jeu) {
        Map<String, List<String>> alertes = new HashMap<>();
        jeu.mesures().forEach((compte, m) -> {
            if (compte.charAt(0) < '1' || compte.charAt(0) > '7') {
                return;
            }
            if (estAnormal(compte, 'D', m.sd(), definitions) || estAnormal(compte, 'C', m.sc(), definitions)) {
                Regle regle = definitions.regle(compte, m.sd().signum() > 0 ? 'D' : 'C');
                if (regle != null && etat.code().equals(regle.etat())) {
                    List<String> liste = alertes.computeIfAbsent(regle.ligne(), k -> new ArrayList<>());
                    if (!liste.contains("SENS_ANORMAL")) {
                        liste.add("SENS_ANORMAL");
                    }
                }
            }
        });
        return alertes;
    }

    /** Un solde est anormal s'il va contre le sens naturel du compte et qu'aucun reclassement ne le prévoit. */
    static boolean estAnormal(String compte, char sens, BigDecimal montant, DefinitionsEtats definitions) {
        if (montant.signum() == 0) {
            return false;
        }
        String naturel = definitions.sensNaturel(compte);
        if (naturel.indexOf(sens) >= 0) {
            return false;
        }
        Regle regle = definitions.regle(compte, sens);
        return regle == null || !regle.reclassement();
    }

    /** Les contrôles de la définition qui portent sur une ligne de cet état. */
    private List<ControleEtatDTO> controles(Etat etat, MoteurEtats moteur) {
        Set<String> codes = new LinkedHashSet<>();
        etat.lignes().forEach(l -> codes.add(l.code()));
        List<ControleEtatDTO> resultats = new ArrayList<>();
        for (Controle controle : definitions.controles()) {
            if (controle.gauche() == null || !porteSur(controle.gauche(), codes)) {
                continue;
            }
            resultats.add(evaluer(controle, moteur));
        }
        return resultats;
    }

    private static boolean porteSur(String expression, Set<String> codes) {
        Matcher m = REFERENCE.matcher(expression);
        while (m.find()) {
            if (codes.contains(m.group(1))) {
                return true;
            }
        }
        return false;
    }

    private static ControleEtatDTO evaluer(Controle controle, MoteurEtats moteur) {
        BigDecimal gauche = moteur.expression(controle.gauche());
        BigDecimal droite = controle.droiteTermes() != null ? moteur.termes(controle.droiteTermes()) : moteur.expression(controle.droite());
        String gravite = ControleService.gravite(controle.gravite());
        if (gauche == null || droite == null) {
            return new ControleEtatDTO(controle.code(), gravite, null, gauche == null ? null : deux(gauche), droite == null ? null : deux(droite), null,
                    "Non effectué : une des deux valeurs n'existe pas.");
        }
        BigDecimal ecart = deux(gauche).subtract(deux(droite));
        return new ControleEtatDTO(controle.code(), gravite, ecart.signum() == 0, deux(gauche), deux(droite), ecart, null);
    }

    private ExerciceDTO exigerExercice(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }

    private Etat exigerEtat(String code) {
        Etat etat = ETATS_SYNTHESE.contains(code) ? definitions.etat(code) : null;
        if (etat == null) {
            throw new ResourceNotFoundException("État", "code", code);
        }
        return etat;
    }

    private static BigDecimal total(List<RepriseLigneDTO> lignes, boolean debit) {
        return lignes.stream().map(l -> debit ? l.soldeDebit() : l.soldeCredit()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal deux(BigDecimal valeur) {
        return valeur.setScale(2, RoundingMode.HALF_UP);
    }
}
