package com.gestion.service.compta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Les états de synthèse tels que les décrit compta/etats_cgnc.json. Chargés une fois : aucune formule n'est écrite dans le code. */
public final class DefinitionsEtats {

    private static final String RESSOURCE = "/compta/etats_cgnc.json";

    /** Une mesure signée sur les comptes qui commencent par un préfixe, sauf les préfixes exclus. */
    public record Terme(String mesure, List<String> prefixes, List<String> sauf, int signe, String natureMouvement, boolean reclassement) {
        public boolean vise(String compte) {
            return prefixes.stream().anyMatch(compte::startsWith) && sauf.stream().noneMatch(compte::startsWith);
        }
    }

    /** Une seule des formes est remplie. Saisie et registre n'ont aucune forme calculée : leur valeur n'existe pas tant que leur lot n'est pas là. */
    public record Forme(List<Terme> termes, List<String> somme, String expr, String n1, String n2, boolean vide, String lisible, boolean saisie) {
    }

    public record Ligne(String code, int ordre, String type, String libelle, String masse, boolean horsTotal, Boolean negatifAutorise, Map<String, Forme> colonnes) {
    }

    public record Colonne(String code, String libelle, boolean precedent) {
    }

    public record Etat(String code, String titre, List<Colonne> colonnes, List<Ligne> lignes) {
    }

    /** Pour un préfixe et un sens : la ligne, la colonne et le signe. */
    public record Regle(String prefixe, List<String> sauf, char sens, String etat, String ligne, String colonne, int signe, boolean reclassement) {
    }

    public record Controle(String code, String gravite, String gauche, String droite, List<Terme> droiteTermes) {
    }

    private static final class Porteur {
        private static final DefinitionsEtats INSTANCE = lire();
    }

    private final String version;
    private final Map<String, Etat> etats = new LinkedHashMap<>();
    private final Map<String, Ligne> lignes = new HashMap<>();
    private final List<Regle> regles = new ArrayList<>();
    private final List<Controle> controles = new ArrayList<>();
    private final Map<String, String> sensNaturel = new HashMap<>();
    private final List<String> comptesASolder = new ArrayList<>();

    public static DefinitionsEtats instance() {
        return Porteur.INSTANCE;
    }

    private static DefinitionsEtats lire() {
        try (InputStream flux = DefinitionsEtats.class.getResourceAsStream(RESSOURCE)) {
            if (flux == null) {
                throw new IllegalStateException("Ressource absente : " + RESSOURCE);
            }
            return new DefinitionsEtats(new ObjectMapper().readTree(flux));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private DefinitionsEtats(JsonNode racine) {
        version = racine.path("version_schema").asText();
        for (String bloc : List.of("etats", "liasse")) {
            racine.path(bloc).fields().forEachRemaining(e -> {
                Etat etat = etat(e.getKey(), e.getValue());
                etats.put(etat.code(), etat);
                etat.lignes().forEach(l -> lignes.put(l.code(), l));
            });
        }
        racine.path("affectation_par_compte").path("regles").forEach(r -> regles.add(new Regle(r.path("prefixe").asText(), textes(r.path("sauf")),
                r.path("sens").asText().charAt(0), r.path("etat").asText(), r.path("ligne").asText(), r.path("colonne").asText(), r.path("signe").asInt(),
                "reclassement".equals(r.path("nature").asText()))));
        racine.path("controles").forEach(c -> controles.add(new Controle(c.path("code").asText(), c.path("gravite").asText(), texte(c, "gauche"), texte(c, "droite"),
                c.has("droite_termes") ? termes(c.path("droite_termes")) : null)));
        racine.path("sens_naturel").path("table").fields().forEachRemaining(e -> sensNaturel.put(e.getKey(), e.getValue().asText()));
        racine.path("comptes_a_solder_en_fin_exercice").forEach(c -> comptesASolder.add(c.path("prefixe").asText()));
    }

    private static Etat etat(String code, JsonNode noeud) {
        List<Colonne> colonnes = new ArrayList<>();
        noeud.path("colonnes").forEach(c -> colonnes.add(new Colonne(c.path("code").asText(), c.path("libelle").asText(), "N-1".equals(c.path("exercice").asText()))));
        List<Ligne> lignes = new ArrayList<>();
        noeud.path("lignes").forEach(l -> lignes.add(ligne(l)));
        lignes.sort(Comparator.comparingInt(Ligne::ordre));
        return new Etat(code, noeud.path("titre").asText(), List.copyOf(colonnes), List.copyOf(lignes));
    }

    private static Ligne ligne(JsonNode l) {
        Map<String, Forme> colonnes = new LinkedHashMap<>();
        l.path("colonnes").fields().forEachRemaining(e -> colonnes.put(e.getKey(), forme(e.getValue())));
        return new Ligne(l.path("code").asText(), l.path("ordre").asInt(), l.path("type").asText(), l.path("libelle").asText(), texte(l, "masse"),
                l.path("hors_total").asBoolean(false), l.has("negatif_autorise") ? l.path("negatif_autorise").asBoolean() : null, colonnes);
    }

    private static Forme forme(JsonNode f) {
        return new Forme(f.has("termes") ? termes(f.path("termes")) : null, f.has("somme") ? textes(f.path("somme")) : null, texte(f, "expr"), texte(f, "n1"), texte(f, "n2"),
                f.path("vide").asBoolean(false), texte(f, "lisible"), f.has("saisie"));
    }

    private static List<Terme> termes(JsonNode noeud) {
        List<Terme> termes = new ArrayList<>();
        noeud.forEach(t -> termes.add(new Terme(t.path("m").asText(), textes(t.path("p")), textes(t.path("sauf")), t.path("s").asInt(), texte(t, "q"),
                "reclassement".equals(t.path("nature").asText()))));
        return List.copyOf(termes);
    }

    private static List<String> textes(JsonNode tableau) {
        List<String> valeurs = new ArrayList<>();
        tableau.forEach(v -> valeurs.add(v.asText()));
        return List.copyOf(valeurs);
    }

    private static String texte(JsonNode noeud, String champ) {
        return noeud.hasNonNull(champ) ? noeud.path(champ).asText() : null;
    }

    public String version() {
        return version;
    }

    public Etat etat(String code) {
        return etats.get(code);
    }

    public Ligne ligne(String code) {
        return lignes.get(code);
    }

    public List<Controle> controles() {
        return controles;
    }

    public List<String> comptesASolder() {
        return comptesASolder;
    }

    /** La règle du préfixe le plus long : 1119 l'emporte sur 111, qui l'exclut de toute façon. */
    public Regle regle(String compte, char sens) {
        Regle retenue = null;
        for (Regle r : regles) {
            if (r.sens() == sens && compte.startsWith(r.prefixe()) && r.sauf().stream().noneMatch(compte::startsWith)
                    && (retenue == null || r.prefixe().length() > retenue.prefixe().length())) {
                retenue = r;
            }
        }
        return retenue;
    }

    /** D, C ou DC, par préfixe le plus long. */
    public String sensNaturel(String compte) {
        for (int longueur = compte.length(); longueur > 0; longueur--) {
            String sens = sensNaturel.get(compte.substring(0, longueur));
            if (sens != null) {
                return sens;
            }
        }
        return "DC";
    }
}
