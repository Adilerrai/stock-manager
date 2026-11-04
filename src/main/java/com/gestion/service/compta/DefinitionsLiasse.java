package com.gestion.service.compta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestion.service.compta.DefinitionsEtats.Terme;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Ce que etats_cgnc.json dit de la liasse, au-delà du calcul : catégorie, registres, blocs libres, cadrages, codes EDI. */
@Component
public class DefinitionsLiasse {

    public record ColonneRegistre(String code, String libelle, String type, Integer edi) {
    }

    public record Bloc(String code, String libelle, String sens, Map<String, Integer> edi) {
    }

    public record Cadrage(String code, String gauche, String droite, List<Terme> droiteTermes) {
    }

    public record Tableau(String code, String titre, String categorie, String etatEtic, Integer tableauEdi, String note, boolean repetable, boolean applicableSous, List<ColonneRegistre> colonnes, List<Bloc> blocs, List<Cadrage> cadrages, Map<String, Integer> totalEdi) {
    }

    public record TableauEdi(int id, List<String> etats) {
    }

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<>();

    static {
        for (String c : List.of("BILAN_ACTIF", "BILAN_PASSIF", "CPC", "ESG_TFR", "T6", "T8", "T9", "T12", "T20", "B1", "BILAN_CONDENSE")) {
            CATEGORIES.put(c, "A");
        }
        for (String c : List.of("T4", "ESG_CAF", "TF_MASSES", "TF_EMPLOIS_RESSOURCES", "T14")) {
            CATEGORIES.put(c, "B");
        }
        for (String c : List.of("T10", "T16", "B6", "B7", "C4")) {
            CATEGORIES.put(c, "C");
        }
        for (String c : List.of("T3", "B13", "C3")) {
            CATEGORIES.put(c, "D");
        }
        for (String c : List.of("T7", "T11", "T13", "T15", "T17", "T18", "T19", "B8", "B9", "B15", "C5", "A0", "A1", "A2", "A3", "IDENTIFICATION")) {
            CATEGORIES.put(c, "E");
        }
    }

    /** Les tableaux qui ne se calculent pas encore : leur raison s'affiche, jamais des zéros. */
    private static final Map<String, String> NOTES_TECHNIQUES = Map.of(
            "B6", "Les totaux viennent du bilan. La ventilation à plus ou moins d'un an, les échus, les devises et les organismes publics ne sont pas encore servis : le module ne suit pas l'échéancier des créances.",
            "B7", "Les totaux viennent du bilan. La ventilation à plus ou moins d'un an, les échus, les devises et les organismes publics ne sont pas encore servis : le module ne suit pas l'échéancier des dettes.",
            "C4", "Les écritures saisies peuvent porter une devise, mais la passerelle n'en reporte aucune : ce tableau n'est pas encore servi.");

    private static final Set<String> SYNTHESE = Set.of("BILAN_ACTIF", "BILAN_PASSIF", "CPC", "ESG_TFR", "ESG_CAF", "TF_MASSES", "TF_EMPLOIS_RESSOURCES");
    private static final Set<String> REGISTRES_APPLICATION = Set.of("T10", "T16");

    private final JsonNode racine;
    private final Map<String, Tableau> tableaux = new LinkedHashMap<>();
    private final List<TableauEdi> tableauxEdi = new ArrayList<>();

    public DefinitionsLiasse() {
        try (InputStream flux = DefinitionsLiasse.class.getResourceAsStream("/compta/etats_cgnc.json")) {
            if (flux == null) {
                throw new IllegalStateException("Ressource absente : etats_cgnc.json");
            }
            racine = new ObjectMapper().readTree(flux);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String bloc : List.of("etats", "liasse")) {
            racine.path(bloc).fields().forEachRemaining(e -> {
                if (CATEGORIES.containsKey(e.getKey())) {
                    tableaux.put(e.getKey(), tableau(e.getKey(), e.getValue()));
                }
            });
        }
        for (JsonNode t : racine.path("edi").path("tableaux")) {
            List<String> etats = new ArrayList<>();
            for (String e : t.path("etat").asText("").split(",")) {
                if (!e.isBlank()) {
                    etats.add(e.trim());
                }
            }
            tableauxEdi.add(new TableauEdi(t.path("id").asInt(), etats));
        }
    }

    private Tableau tableau(String code, JsonNode n) {
        List<ColonneRegistre> colonnes = new ArrayList<>();
        n.path("colonnes").forEach(c -> colonnes.add(new ColonneRegistre(c.path("code").asText(), c.path("libelle").asText(), c.path("type").asText("texte"), c.hasNonNull("edi") ? c.path("edi").asInt() : null)));
        List<Bloc> blocs = new ArrayList<>();
        n.path("blocs_libres").forEach(b -> blocs.add(new Bloc(b.path("code").asText(), b.path("libelle").asText(), b.path("sens").asText("+"), entiers(b.path("edi")))));
        List<Cadrage> cadrages = new ArrayList<>();
        n.path("cadrages").forEach(c -> cadrages.add(new Cadrage(c.path("code").asText(), c.path("gauche").asText(null), c.path("droite").asText(null), termes(c.path("droite_termes")))));
        return new Tableau(code, n.path("titre").asText(), CATEGORIES.get(code), n.path("etat_etic").asText(null), n.hasNonNull("tableau_edi") ? n.path("tableau_edi").asInt() : null, n.path("note").asText(null),
                n.hasNonNull("lignes_repetables"), n.hasNonNull("applicable_si"), colonnes, blocs, cadrages, entiers(n.path("total").path("edi")));
    }

    private static Map<String, Integer> entiers(JsonNode n) {
        Map<String, Integer> valeurs = new LinkedHashMap<>();
        n.fields().forEachRemaining(e -> valeurs.put(e.getKey(), e.getValue().asInt()));
        return valeurs;
    }

    private static List<Terme> termes(JsonNode n) {
        if (n.isMissingNode() || !n.isArray()) {
            return null;
        }
        List<Terme> termes = new ArrayList<>();
        n.forEach(t -> {
            List<String> prefixes = new ArrayList<>();
            t.path("p").forEach(p -> prefixes.add(p.asText()));
            List<String> sauf = new ArrayList<>();
            t.path("sauf").forEach(p -> sauf.add(p.asText()));
            termes.add(new Terme(t.path("m").asText(), prefixes, sauf, t.path("s").asInt(), t.path("q").asText(null), false));
        });
        return termes;
    }

    public List<Tableau> tableaux() {
        return List.copyOf(tableaux.values());
    }

    public Tableau tableau(String code) {
        return tableaux.get(code);
    }

    public boolean estSynthese(String code) {
        return SYNTHESE.contains(code);
    }

    public boolean registreDeLApplication(String code) {
        return REGISTRES_APPLICATION.contains(code);
    }

    public String noteTechnique(String code) {
        return NOTES_TECHNIQUES.get(code);
    }

    public List<TableauEdi> tableauxEdi() {
        return tableauxEdi;
    }

    /** Les cellules d'un état qui portent un code EDI : ligne, colonne, code. */
    public List<Object[]> cellulesEdi(String etat) {
        JsonNode n = racine.path("etats").path(etat);
        if (n.isMissingNode()) {
            n = racine.path("liasse").path(etat);
        }
        List<Object[]> cellules = new ArrayList<>();
        n.path("lignes").forEach(l -> l.path("colonnes").fields().forEachRemaining(c -> {
            if (c.getValue().hasNonNull("edi")) {
                cellules.add(new Object[]{l.path("code").asText(), c.getKey(), c.getValue().path("edi").asInt()});
            }
        }));
        return cellules;
    }

    public List<String[]> champsSupplementaires(int tableau) {
        List<String[]> champs = new ArrayList<>();
        racine.path("edi").path("extra_fields").forEach(f -> {
            if (f.path("tableau").asInt() == tableau) {
                champs.add(new String[]{f.path("code").asText(), f.path("valeur").asText()});
            }
        });
        return champs;
    }

    public String modeleEdi() {
        return racine.path("modele").asText("NORMAL");
    }
}
