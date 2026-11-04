package com.gestion.service.compta;

import com.acommon.exception.CommonException;
import com.acommon.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gestion.persistent.dto.compta.EtatDtos.ColonneEtatDTO;
import com.gestion.persistent.dto.compta.EtatDtos.LigneEtatDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.BlocLibreDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.CadrageDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.CelluleSaisieDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.ColonneRegistreDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.DepotLiasseRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.LiasseDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.LigneRegistreDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.RegistreRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.SaisiesRequest;
import com.gestion.persistent.dto.compta.FiscalDtos.TableauDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.TableauResumeDTO;
import com.gestion.persistent.dto.compta.FiscalDtos.ValeurRegistreDTO;
import com.gestion.persistent.dto.compta.ReferentielDtos.ExerciceDTO;
import com.gestion.persistent.enums.ActionAudit;
import com.gestion.repository.compta.ClotureRepository;
import com.gestion.repository.compta.ExerciceRepository;
import com.gestion.repository.compta.FiscalComptaRepository;
import com.gestion.repository.compta.FiscalComptaRepository.Liasse;
import com.gestion.repository.compta.FiscalComptaRepository.Registre;
import com.gestion.repository.compta.LivreRepository;
import com.gestion.service.AuditService;
import com.gestion.service.compta.DefinitionsEtats.Etat;
import com.gestion.service.compta.DefinitionsEtats.Forme;
import com.gestion.service.compta.DefinitionsEtats.Ligne;
import com.gestion.service.compta.DefinitionsLiasse.Bloc;
import com.gestion.service.compta.DefinitionsLiasse.Cadrage;
import com.gestion.service.compta.DefinitionsLiasse.ColonneRegistre;
import com.gestion.service.compta.DefinitionsLiasse.Tableau;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La liasse, tableau par tableau, avec le statut de chacun. Un tableau non servi porte une note technique et sa raison, jamais des zéros (avis n° 24).
 * Le fichier XML reste marqué non validé : aucun document de la DGI sur l'EDI de la liasse IS n'a été lu.
 */
@Service
public class LiasseComptaService {

    public static final String MARQUE_NON_VALIDE = "NON VALIDE : aucun document de la DGI sur l'EDI de la liasse IS n'a ete lu. Ce fichier n'est pas une declaration.";
    /** Ces tableaux portent des CIN et des adresses : ni leur contenu ni leurs lignes ne se journalisent. */
    private static final Set<String> DONNEES_PERSONNELLES = Set.of("T13", "T18", "T19");

    private final FiscalComptaRepository repository;
    private final DefinitionsLiasse liasse;
    private final DefinitionsEtats definitions = DefinitionsEtats.instance();
    private final EtatService etats;
    private final ExerciceRepository exercices;
    private final LivreRepository livres;
    private final ClotureRepository clotures;
    private final LivreCourantService livreCourant;
    private final AuditService audit;
    private final ObjectMapper json;

    public LiasseComptaService(FiscalComptaRepository repository, DefinitionsLiasse liasse, EtatService etats, ExerciceRepository exercices, LivreRepository livres, ClotureRepository clotures, LivreCourantService livreCourant, AuditService audit, ObjectMapper json) {
        this.clotures = clotures;
        this.repository = repository;
        this.liasse = liasse;
        this.etats = etats;
        this.exercices = exercices;
        this.livres = livres;
        this.livreCourant = livreCourant;
        this.audit = audit;
        this.json = json;
    }

    private ExerciceDTO exiger(Long livreId, Long id) {
        return exercices.lire(livreId, id).orElseThrow(() -> new ResourceNotFoundException("Exercice", "id", id));
    }

    private Tableau exigerTableau(String code) {
        Tableau t = liasse.tableau(code);
        if (t == null) {
            throw new ResourceNotFoundException("Tableau", "code", code);
        }
        return t;
    }

    // --- Le moteur, avec les saisies superposées

    private static String blocDe(String codeLigne, Tableau t) {
        if (t != null) {
            for (Bloc b : t.blocs()) {
                if (codeLigne.startsWith(b.code() + ".")) {
                    return b.code();
                }
            }
        }
        return null;
    }

    private MoteurEtats.Saisies saisies(Long livreId, Long exerciceId, String tableauRegistre) {
        Map<String, BigDecimal> cellules = new HashMap<>();
        Map<String, BigDecimal> blocs = new HashMap<>();
        Tableau t3 = liasse.tableau("T3");
        for (CelluleSaisieDTO s : repository.saisies(livreId, exerciceId)) {
            if (s.valeurNum() == null) {
                continue;
            }
            String bloc = blocDe(s.codeLigne(), t3);
            if (bloc != null) {
                blocs.merge(bloc + "|" + s.colonne(), s.valeurNum(), BigDecimal::add);
            } else {
                cellules.put(s.codeLigne() + "|" + s.colonne(), s.valeurNum());
            }
        }
        if (tableauRegistre != null) {
            for (LigneRegistreDTO l : lignesRegistre(livreId, exerciceId, tableauRegistre)) {
                for (ValeurRegistreDTO v : l.valeurs()) {
                    try {
                        if (v.valeur() != null && !v.valeur().isBlank()) {
                            blocs.merge(v.colonne(), new BigDecimal(v.valeur()), BigDecimal::add);
                        }
                    } catch (NumberFormatException ignore) {
                        // Une colonne de texte n'entre pas dans une somme.
                    }
                }
            }
        }
        return new MoteurEtats.Saisies(cellules, blocs);
    }

    private MoteurEtats moteur(Long livreId, ExerciceDTO e, String tableau) {
        return new MoteurEtats(definitions, etats.pile(livreId, e, e.au()), saisies(livreId, e.id(), tableau));
    }

    // --- Liste

    @Transactional(readOnly = true)
    public LiasseDTO liste(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        Map<String, Long> registres = repository.nombresRegistres(livreId, exerciceId);
        List<CelluleSaisieDTO> saisies = repository.saisies(livreId, exerciceId);
        List<TableauResumeDTO> resumes = new ArrayList<>();
        for (Tableau t : liasse.tableaux()) {
            List<CadrageDTO> cadrages = t.cadrages().isEmpty() ? List.of() : cadrages(moteur(livreId, e, t.code()), t);
            resumes.add(new TableauResumeDTO(t.code(), t.titre(), statut(t, registres, saisies), t.categorie(), liasse.noteTechnique(t.code()), t.etatEtic(), t.tableauEdi(), cadrages));
        }
        var existante = repository.liasse(livreId, exerciceId);
        return new LiasseDTO(existante.map(Liasse::version).orElse(1), existante.map(Liasse::statut).orElse("BROUILLON"), existante.map(Liasse::nonValideEdi).orElse(true), existante.map(Liasse::sha256).orElse(null),
                existante.map(Liasse::deposeLe).orElse(null), existante.map(Liasse::numeroRecu).orElse(null), resumes);
    }

    private String statut(Tableau t, Map<String, Long> registres, List<CelluleSaisieDTO> saisies) {
        if (liasse.noteTechnique(t.code()) != null) {
            return "NOTE_TECHNIQUE";
        }
        if ("A".equals(t.categorie()) || "B".equals(t.categorie()) || "C".equals(t.categorie()) || "IDENTIFICATION".equals(t.code())) {
            return "CALCULE";
        }
        Etat etat = definitions.etat(t.code());
        Set<String> lignes = new java.util.HashSet<>();
        etat.lignes().forEach(l -> lignes.add(l.code()));
        boolean saisi = registres.getOrDefault(t.code(), 0L) > 0 || saisies.stream().anyMatch(s -> lignes.contains(s.codeLigne()) || t.blocs().stream().anyMatch(b -> s.codeLigne().startsWith(b.code() + ".")));
        if (saisi) {
            return "SAISI";
        }
        return t.applicableSous() ? "NON_APPLICABLE" : "A_COMPLETER";
    }

    private List<CadrageDTO> cadrages(MoteurEtats moteur, Tableau t) {
        List<CadrageDTO> resultat = new ArrayList<>();
        for (Cadrage c : t.cadrages()) {
            BigDecimal g = c.gauche() == null ? null : moteur.expression(c.gauche());
            BigDecimal d = c.droite() != null ? moteur.expression(c.droite()) : c.droiteTermes() != null ? moteur.termes(c.droiteTermes()) : null;
            if (g == null || d == null) {
                resultat.add(new CadrageDTO(c.code(), null, g, d, "Une des deux valeurs n'est pas servie."));
            } else {
                resultat.add(new CadrageDTO(c.code(), g.subtract(d).abs().compareTo(new BigDecimal("0.01")) <= 0, g.setScale(2, RoundingMode.HALF_UP), d.setScale(2, RoundingMode.HALF_UP), null));
            }
        }
        return resultat;
    }

    // --- Un tableau

    @Transactional(readOnly = true)
    public TableauDTO tableau(Long exerciceId, String code) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        Tableau t = exigerTableau(code);
        Etat etat = definitions.etat(code);
        MoteurEtats moteur = moteur(livreId, e, code);
        Map<String, String> textes = new HashMap<>();
        List<CelluleSaisieDTO> saisies = repository.saisiesDuTableau(livreId, exerciceId, code);
        saisies.forEach(s -> textes.put(s.codeLigne() + "|" + s.colonne(), s.valeurTxt() != null ? s.valeurTxt() : s.valeurNum() == null ? null : s.valeurNum().toPlainString()));
        Map<String, String> identite = "IDENTIFICATION".equals(code) ? identite(livreId, e) : Map.of();
        List<LigneEtatDTO> lignes = new ArrayList<>();
        List<String> saisissables = new ArrayList<>();
        for (Ligne l : etat.lignes()) {
            if ("ligne_libre".equals(l.type())) {
                continue;
            }
            Map<String, String> valeurs = new LinkedHashMap<>();
            for (var c : etat.colonnes()) {
                Forme f = l.colonnes().get(c.code());
                boolean aSaisir = (f != null && f.saisie()) || ("saisie".equals(l.type()) && l.colonnes().isEmpty());
                if (aSaisir) {
                    saisissables.add(l.code() + "|" + c.code());
                    valeurs.put(c.code(), textes.get(l.code() + "|" + c.code()));
                    continue;
                }
                if (identite.containsKey(l.code())) {
                    valeurs.put(c.code(), identite.get(l.code()));
                    continue;
                }
                BigDecimal v = f == null || f.vide() ? null : moteur.cellule(l.code(), c.code());
                valeurs.put(c.code(), v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
            lignes.add(new LigneEtatDTO(l.code(), l.libelle(), l.type(), 2, l.masse(), l.horsTotal(), valeurs, List.of()));
        }
        boolean applique = t.repetable();
        List<ColonneRegistreDTO> colonnesRegistre = applique ? t.colonnes().stream().map(c -> new ColonneRegistreDTO(c.code(), c.libelle(), c.type())).toList() : List.of();
        List<LigneRegistreDTO> registre = applique ? lignesRegistre(livreId, exerciceId, code) : List.of();
        List<ColonneEtatDTO> colonnes = etat.colonnes().stream().map(c -> new ColonneEtatDTO(c.code(), c.libelle())).toList();
        Map<String, Long> registres = repository.nombresRegistres(livreId, exerciceId);
        return new TableauDTO(t.code(), t.titre(), statut(t, registres, repository.saisies(livreId, exerciceId)), t.categorie(), liasse.noteTechnique(code), t.note(), colonnes, lignes, saisissables,
                t.blocs().stream().map(b -> new BlocLibreDTO(b.code(), b.libelle(), b.sens())).toList(), saisies, colonnesRegistre, registre, liasse.registreDeLApplication(code), cadrages(moteur, t), "OUVERT".equals(e.statut()) || "ARRETE".equals(e.statut()));
    }

    private Map<String, String> identite(Long livreId, ExerciceDTO e) {
        var id = livres.identite(livreId);
        Map<String, String> v = new HashMap<>();
        v.put("ID.DENOMINATION", id.raisonSociale());
        v.put("ID.SIEGE", id.adresse());
        v.put("ID.EXERCICE_DU", e.du().toString());
        v.put("ID.EXERCICE_AU", e.au().toString());
        v.put("ID.IF", id.identifiantFiscal());
        v.put("ID.TP", id.taxeProfessionnelle());
        v.put("ID.RC", id.registreCommerce());
        v.put("ID.LIEU", id.ville());
        return v;
    }

    private List<LigneRegistreDTO> lignesRegistre(Long livreId, Long exerciceId, String code) {
        if ("T10".equals(code)) {
            return registreT10(livreId, exerciceId);
        }
        if ("T16".equals(code)) {
            return registreT16(livreId, exerciceId);
        }
        List<LigneRegistreDTO> lignes = new ArrayList<>();
        for (Registre r : repository.registre(livreId, exerciceId, code)) {
            List<ValeurRegistreDTO> valeurs = new ArrayList<>();
            try {
                json.readTree(r.donnees()).fields().forEachRemaining(f -> valeurs.add(new ValeurRegistreDTO(f.getKey(), f.getValue().isNull() ? null : f.getValue().asText())));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException(ex);
            }
            lignes.add(new LigneRegistreDTO(r.numero(), valeurs));
        }
        return lignes;
    }

    private static ValeurRegistreDTO valeur(String colonne, Object v) {
        return new ValeurRegistreDTO(colonne, v == null ? null : v instanceof java.sql.Date d ? d.toLocalDate().toString() : v.toString());
    }

    /** T10 : les sorties du registre, jamais une saisie. */
    private List<LigneRegistreDTO> registreT10(Long livreId, Long exerciceId) {
        List<LigneRegistreDTO> lignes = new ArrayList<>();
        int n = 1;
        for (Map<String, Object> s : repository.sorties(livreId, exerciceId)) {
            BigDecimal pmv = (BigDecimal) s.get("plus_ou_moins_value");
            lignes.add(new LigneRegistreDTO(n++, List.of(valeur("DATE", s.get("date_sortie")), valeur("COMPTE", s.get("compte_immo")), valeur("BRUT", s.get("valeur_brute")), valeur("AMORT", s.get("amortissements_cumules")), valeur("VNA", s.get("valeur_nette")),
                    valeur("PRODUIT", s.get("prix_cession_ht")), valeur("PLUS", pmv.signum() > 0 ? pmv : null), valeur("MOINS", pmv.signum() < 0 ? pmv.negate() : null))));
        }
        return lignes;
    }

    /** T16 : une ligne par fiche, la dotation lue sur ce qui a été passé. */
    private List<LigneRegistreDTO> registreT16(Long livreId, Long exerciceId) {
        List<LigneRegistreDTO> lignes = new ArrayList<>();
        int n = 1;
        for (Map<String, Object> f : repository.dotationsParFiche(livreId, exerciceId)) {
            Number duree = (Number) f.get("duree_mois");
            BigDecimal anterieurs = ((BigDecimal) f.get("cumul_reprise")).add((BigDecimal) f.get("anterieures"));
            BigDecimal taux = duree == null || duree.intValue() == 0 ? null : new BigDecimal("1200").divide(BigDecimal.valueOf(duree.longValue()), 2, RoundingMode.HALF_UP);
            lignes.add(new LigneRegistreDTO(n++, List.of(valeur("IMMOBILISATION", f.get("designation")), valeur("DATE_ENTREE", f.get("date_acquisition")), valeur("PRIX", f.get("valeur_brute")), valeur("AMORT_ANTERIEURS", anterieurs), valeur("TAUX", taux), valeur("DUREE", duree), valeur("AMORT_EXERCICE", f.get("exercice")), valeur("TOTAL_FIN", anterieurs.add((BigDecimal) f.get("exercice"))))));
        }
        return lignes;
    }

    // --- Saisies

    @Transactional
    public TableauDTO saisir(Long exerciceId, String code, SaisiesRequest r) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        Tableau t = exigerTableau(code);
        exigerModifiable(e);
        for (CelluleSaisieDTO c : r.cellules()) {
            if (IsComptaService.LIGNE_IS_T3.equals(c.codeLigne())) {
                throw new CommonException("Cette ligne vient de la validation de l'IS.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LIASSE_SAISIE");
            }
            boolean duTableau = definitions.ligne(c.codeLigne()) != null || t.blocs().stream().anyMatch(b -> c.codeLigne().startsWith(b.code() + "."));
            if (!duTableau) {
                throw new CommonException("La ligne " + c.codeLigne() + " n'appartient pas au tableau " + code + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LIASSE_SAISIE");
            }
            if (c.valeurNum() == null && (c.valeurTxt() == null || c.valeurTxt().isBlank())) {
                repository.retirerSaisie(livreId, exerciceId, code, c.codeLigne(), c.colonne());
            } else {
                repository.ecrireSaisie(livreId, exerciceId, code, c, livreCourant.auteur());
            }
        }
        audit.logAction(ActionAudit.MODIFICATION, "compta_liasse_saisies", exerciceId, "Tableau " + code + " : " + r.cellules().size() + " cellule(s)");
        return tableau(exerciceId, code);
    }

    /** Remplace le registre d'un tableau. Rien de son contenu n'entre dans la trace : T13, T18 et T19 portent des données personnelles. */
    @Transactional
    public TableauDTO enregistrerRegistre(Long exerciceId, String code, RegistreRequest r) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        Tableau t = exigerTableau(code);
        exigerModifiable(e);
        if (!t.repetable() || liasse.registreDeLApplication(code)) {
            throw new CommonException("Le tableau " + code + " ne se saisit pas en registre.", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LIASSE_SAISIE");
        }
        Set<String> colonnes = new java.util.HashSet<>();
        t.colonnes().forEach(c -> colonnes.add(c.code()));
        List<Registre> lignes = new ArrayList<>();
        for (LigneRegistreDTO l : r.lignes()) {
            ObjectNode donnees = json.createObjectNode();
            for (ValeurRegistreDTO v : l.valeurs()) {
                if (!colonnes.contains(v.colonne())) {
                    throw new CommonException("Colonne inconnue : " + v.colonne() + ".", HttpStatus.UNPROCESSABLE_ENTITY, "COMPTA_LIASSE_SAISIE");
                }
                donnees.put(v.colonne(), v.valeur());
            }
            lignes.add(new Registre(l.numero(), donnees.toString()));
        }
        repository.remplacerRegistre(livreId, exerciceId, code, lignes, livreCourant.auteur());
        audit.logAction(ActionAudit.MODIFICATION, "compta_liasse_registres", exerciceId, "Registre " + code + " : " + lignes.size() + " ligne(s)" + (DONNEES_PERSONNELLES.contains(code) ? " (données personnelles, non reprises ici)" : ""));
        return tableau(exerciceId, code);
    }

    private static void exigerModifiable(ExerciceDTO e) {
        if ("CLOTURE".equals(e.statut())) {
            throw new CommonException("L'exercice " + e.code() + " est clos : sa liasse ne se modifie plus.", HttpStatus.CONFLICT, "COMPTA_EXERCICE_CLOS");
        }
    }

    // --- Le fichier

    /** Produit le XML selon le squelette du classeur. Il reste marqué non validé, dans le fichier comme à l'écran. */
    @Transactional
    public byte[] produireXml(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        byte[] xml = xml(livreId, e);
        repository.ecrireXml(livreId, exerciceId, clotures.dernierFigeage(livreId, exerciceId), liasse.modeleEdi(), sha256(xml), xml);
        audit.logAction(ActionAudit.MODIFICATION, "compta_liasses", exerciceId, "XML de la liasse " + e.code() + " produit, marqué non validé");
        return xml;
    }

    @Transactional(readOnly = true)
    public byte[] fichierXml(Long exerciceId) {
        Long livreId = livreCourant.exiger().livreId();
        exiger(livreId, exerciceId);
        return repository.liasse(livreId, exerciceId).map(Liasse::xml).filter(x -> x != null).orElseThrow(() -> new CommonException("Produisez d'abord le fichier de la liasse.", HttpStatus.CONFLICT, "COMPTA_LIASSE_SANS_FICHIER"));
    }

    private byte[] xml(Long livreId, ExerciceDTO e) {
        var id = livres.identite(livreId);
        StringBuilder x = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<!-- ").append(MARQUE_NON_VALIDE).append(" -->\n<Liasse>\n  <modele><id>1</id></modele>\n");
        x.append("  <resultatFiscal><identifiantFiscal>").append(echapper(id.identifiantFiscal())).append("</identifiantFiscal><exerciceFiscalDu>").append(e.du()).append("</exerciceFiscalDu><exerciceFiscalAu>").append(e.au()).append("</exerciceFiscalAu></resultatFiscal>\n  <groupeValeursTableau>\n");
        for (var t : liasse.tableauxEdi()) {
            x.append("    <ValeursTableau>\n      <tableau><id>").append(t.id()).append("</id></tableau>\n      <groupeValeurs>\n");
            for (String etat : t.etats()) {
                ecrireCellules(x, livreId, e, etat);
            }
            x.append("      </groupeValeurs>\n      <extraFieldvaleurs>");
            for (String[] champ : liasse.champsSupplementaires(t.id())) {
                if (champ[1].contains("fin d'exercice")) {
                    x.append("<ExtraFieldValeur><extraField><code>").append(champ[0]).append("</code></extraField><valeur>").append(e.au()).append("</valeur></ExtraFieldValeur>");
                }
            }
            x.append("</extraFieldvaleurs>\n    </ValeursTableau>\n");
        }
        x.append("  </groupeValeursTableau>\n</Liasse>\n");
        return x.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void ecrireCellules(StringBuilder x, Long livreId, ExerciceDTO e, String etat) {
        if (definitions.etat(etat) == null) {
            return;
        }
        MoteurEtats moteur = moteur(livreId, e, etat);
        Map<String, String> textes = new HashMap<>();
        repository.saisiesDuTableau(livreId, e.id(), etat).forEach(s -> textes.put(s.codeLigne() + "|" + s.colonne(), s.valeurTxt() != null ? s.valeurTxt() : s.valeurNum() == null ? null : s.valeurNum().toPlainString()));
        for (Object[] c : liasse.cellulesEdi(etat)) {
            String ligne = (String) c[0];
            String colonne = (String) c[1];
            Forme f = definitions.ligne(ligne).colonnes().get(colonne);
            String valeur = textes.get(ligne + "|" + colonne);
            if (valeur == null && (f == null || !f.vide())) {
                BigDecimal v = moteur.cellule(ligne, colonne);
                valeur = v == null ? null : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
            }
            if (valeur != null) {
                cellule(x, (Integer) c[2], valeur, null);
            }
        }
        Tableau t = liasse.tableau(etat);
        if (t != null) {
            for (Bloc b : t.blocs()) {
                ecrireBloc(x, livreId, e, etat, b);
            }
            if (t.repetable()) {
                for (LigneRegistreDTO l : lignesRegistre(livreId, e.id(), etat)) {
                    for (ValeurRegistreDTO v : l.valeurs()) {
                        ColonneRegistre col = t.colonnes().stream().filter(k -> k.code().equals(v.colonne())).findFirst().orElse(null);
                        if (col != null && col.edi() != null && v.valeur() != null) {
                            cellule(x, col.edi(), v.valeur(), l.numero());
                        }
                    }
                }
            }
        }
    }

    private void ecrireBloc(StringBuilder x, Long livreId, ExerciceDTO e, String etat, Bloc b) {
        Map<String, Map<String, String>> parLigne = new LinkedHashMap<>();
        for (CelluleSaisieDTO s : repository.saisiesDuTableau(livreId, e.id(), etat)) {
            if (s.codeLigne().startsWith(b.code() + ".")) {
                Map<String, String> ligne = parLigne.computeIfAbsent(s.codeLigne(), k -> new LinkedHashMap<>());
                if (s.valeurNum() != null) {
                    ligne.put(s.colonne(), s.valeurNum().toPlainString());
                }
                if (s.valeurTxt() != null) {
                    ligne.put("LIBELLE", s.valeurTxt());
                }
            }
        }
        int n = 1;
        for (Map<String, String> ligne : parLigne.values()) {
            for (Map.Entry<String, String> v : ligne.entrySet()) {
                Integer code = b.edi().get(v.getKey());
                if (code != null) {
                    cellule(x, code, v.getValue(), n);
                }
            }
            n++;
        }
    }

    private static void cellule(StringBuilder x, int code, String valeur, Integer numero) {
        x.append("        <ValeurCellule><cellule><codeEdi>").append(code).append("</codeEdi></cellule><valeur>").append(echapper(valeur)).append("</valeur>");
        if (numero != null) {
            x.append("<numeroLigne>").append(numero).append("</numeroLigne>");
        }
        x.append("</ValeurCellule>\n");
    }

    private static String echapper(String v) {
        return v == null ? "" : v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String sha256(byte[] contenu) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenu));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public LiasseDTO deposer(Long exerciceId, DepotLiasseRequest r) {
        Long livreId = livreCourant.exiger().livreId();
        ExerciceDTO e = exiger(livreId, exerciceId);
        Liasse l = repository.liasse(livreId, exerciceId).filter(x -> x.xml() != null).orElseThrow(() -> new CommonException("Produisez d'abord le fichier de la liasse.", HttpStatus.CONFLICT, "COMPTA_LIASSE_SANS_FICHIER"));
        repository.deposerLiasse(livreId, l.id(), r.deposeLe(), r.numeroRecu(), livreCourant.auteur());
        audit.logAction(ActionAudit.MODIFICATION, "compta_liasses", exerciceId, "Liasse " + e.code() + " déposée le " + r.deposeLe());
        return liste(exerciceId);
    }
}
