package com.gestion.service.compta;

import com.gestion.service.compta.DefinitionsEtats.Forme;
import com.gestion.service.compta.DefinitionsEtats.Ligne;
import com.gestion.service.compta.DefinitionsEtats.Terme;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Évalue les formules des états sur des mesures reçues en paramètre. Fonction pure : ni dépôt, ni Spring, et aucune ligne d'écriture.
 * <p>
 * Une valeur nulle dit « pas de valeur », jamais zéro : la colonne N-1 d'un exercice absent reste vide, et tout calcul qui en dépend aussi.
 */
public final class MoteurEtats {

    public record MesureCompte(BigDecimal od, BigDecimal oc, BigDecimal md, BigDecimal mc, BigDecimal sd, BigDecimal sc) {
    }

    /** Lignes portant une nature de mouvement ou marquées hors flux : le reste des mouvements n'en a pas besoin. */
    public record Marquee(String compte, String nature, boolean horsFlux, BigDecimal md, BigDecimal mc) {
    }

    public record JeuMesures(Map<String, MesureCompte> mesures, List<Marquee> marquees) {
    }

    /** Ce qu'un compte apporte à une cellule, signe compris. */
    public record Contribution(String compte, String mesure, BigDecimal montant, boolean reclassement) {
    }

    private static final Pattern JETON = Pattern.compile("\\s*(?:(\\d+(?:\\.\\d+)?)|([A-Za-z_][A-Za-z0-9_.]*)(\\[[A-Z0-9_]+])?|([()+\\-*/,]))");
    private static final Pattern EXPRESSION_LINEAIRE = Pattern.compile("^[+-]?[A-Za-z0-9_.]+\\[[A-Z0-9_]+](?:[+-][A-Za-z0-9_.]+\\[[A-Z0-9_]+])*$");

    private final DefinitionsEtats definitions;
    private final List<JeuMesures> jeux;
    private final Map<String, BigDecimal> memoire = new HashMap<>();
    private final Saisies saisies;

    /**
     * Ce que la liasse saisit : une cellule par « ligne|colonne », et le total d'un bloc libre ou d'un registre par « bloc|colonne ».
     * Une cellule à saisir restée vide compte pour zéro dans un calcul ; une cellule de registre non servie laisse le calcul sans valeur.
     */
    public record Saisies(Map<String, BigDecimal> cellules, Map<String, BigDecimal> blocs) {
    }

    /** @param jeux N d'abord, puis N-1 et N-2 ; un élément nul quand l'exercice n'existe pas et qu'aucune balance n'a été reprise */
    public MoteurEtats(DefinitionsEtats definitions, List<JeuMesures> jeux) {
        this(definitions, jeux, null);
    }

    public MoteurEtats(DefinitionsEtats definitions, List<JeuMesures> jeux, Saisies saisies) {
        this.definitions = definitions;
        this.jeux = new ArrayList<>(jeux);
        this.saisies = saisies;
    }

    public BigDecimal cellule(String ligne, String colonne) {
        return cellule(ligne, colonne, 0);
    }

    public BigDecimal expression(String texte) {
        return expression(texte, 0);
    }

    public BigDecimal termes(List<Terme> termes) {
        return termes(termes, 0);
    }

    /** La formule d'une cellule, comme elle se lit dans la définition. */
    public String formule(String ligne, String colonne) {
        Forme forme = forme(ligne, colonne);
        if (forme == null) {
            return null;
        }
        if (forme.lisible() != null) {
            return forme.lisible();
        }
        if (forme.expr() != null) {
            return forme.expr();
        }
        if (forme.somme() != null) {
            return String.join(" + ", forme.somme());
        }
        if (forme.n1() != null) {
            return forme.n1() + " de l'exercice précédent";
        }
        return forme.n2() != null ? forme.n2() + " de l'exercice N-2" : null;
    }

    /** Les comptes qui composent une cellule. Une formule qui n'est pas une simple somme signée n'a pas de détail par compte. */
    public List<Contribution> contributions(String ligne, String colonne) {
        Map<String, Contribution> parCle = new TreeMap<>();
        collecter(ligne, colonne, 0, 1, parCle);
        return parCle.values().stream().filter(c -> c.montant().signum() != 0).toList();
    }

    private void collecter(String ligne, String colonne, int decalage, int signe, Map<String, Contribution> sortie) {
        Forme forme = forme(ligne, colonne);
        if (forme == null || forme.vide()) {
            return;
        }
        if (forme.termes() != null) {
            JeuMesures jeu = jeu(decalage);
            if (jeu == null) {
                return;
            }
            for (Terme terme : forme.termes()) {
                for (String compte : jeu.mesures().keySet()) {
                    if (terme.vise(compte)) {
                        BigDecimal montant = valeurCompte(jeu, compte, terme).multiply(BigDecimal.valueOf((long) terme.signe() * signe));
                        sortie.merge(compte + "|" + terme.mesure(), new Contribution(compte, terme.mesure(), montant, terme.reclassement()),
                                (a, b) -> new Contribution(a.compte(), a.mesure(), a.montant().add(b.montant()), a.reclassement() || b.reclassement()));
                    }
                }
            }
        } else if (forme.somme() != null) {
            forme.somme().forEach(l -> collecter(l, colonne, decalage, signe, sortie));
        } else if (forme.n1() != null) {
            collecter(ligne, forme.n1(), decalage + 1, signe, sortie);
        } else if (forme.n2() != null) {
            collecter(ligne, forme.n2(), decalage + 2, signe, sortie);
        } else if (forme.expr() != null && EXPRESSION_LINEAIRE.matcher(forme.expr().replace(" ", "")).matches()) {
            int signeCourant = signe;
            for (Jeton jeton : jetons(forme.expr())) {
                if ("op".equals(jeton.genre())) {
                    signeCourant = "-".equals(jeton.texte()) ? -signe : signe;
                } else {
                    collecter(jeton.texte(), jeton.colonne(), decalage, signeCourant, sortie);
                }
            }
        }
    }

    private BigDecimal cellule(String ligne, String colonne, int decalage) {
        String cle = ligne + "|" + colonne + "|" + decalage;
        if (memoire.containsKey(cle)) {
            return memoire.get(cle);
        }
        BigDecimal valeur = calculer(ligne, colonne, decalage);
        memoire.put(cle, valeur);
        return valeur;
    }

    private BigDecimal calculer(String ligne, String colonne, int decalage) {
        Forme forme = forme(ligne, colonne);
        if (saisies != null && decalage == 0) {
            BigDecimal saisie = saisies.cellules().get(ligne + "|" + colonne);
            if (saisie != null) {
                return saisie;
            }
        }
        if (forme == null || forme.vide()) {
            return null;
        }
        if (forme.saisie()) {
            return saisies != null ? BigDecimal.ZERO : null;
        }
        if (forme.termes() != null) {
            return termes(forme.termes(), decalage);
        }
        if (forme.somme() != null) {
            return somme(forme.somme(), colonne, decalage);
        }
        if (forme.expr() != null) {
            return expression(forme.expr(), decalage);
        }
        if (forme.n1() != null) {
            return cellule(ligne, forme.n1(), decalage + 1);
        }
        return forme.n2() != null ? cellule(ligne, forme.n2(), decalage + 2) : null;
    }

    private Forme forme(String ligne, String colonne) {
        Ligne definition = definitions.ligne(ligne);
        if (definition == null) {
            throw new IllegalArgumentException("Ligne inconnue : " + ligne);
        }
        return definition.colonnes().get(colonne);
    }

    private JeuMesures jeu(int decalage) {
        return decalage < jeux.size() ? jeux.get(decalage) : null;
    }

    private BigDecimal termes(List<Terme> termes, int decalage) {
        JeuMesures jeu = jeu(decalage);
        if (jeu == null) {
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Terme terme : termes) {
            for (String compte : jeu.mesures().keySet()) {
                if (terme.vise(compte)) {
                    total = total.add(valeurCompte(jeu, compte, terme).multiply(BigDecimal.valueOf(terme.signe())));
                }
            }
        }
        return total;
    }

    private static BigDecimal valeurCompte(JeuMesures jeu, String compte, Terme terme) {
        String mesure = terme.mesure();
        if (terme.natureMouvement() != null) {
            return marquees(jeu, compte, mesure, terme.natureMouvement());
        }
        MesureCompte m = jeu.mesures().get(compte);
        return switch (mesure) {
            case "SD" -> m.sd();
            case "SC" -> m.sc();
            case "OD" -> m.od();
            case "OC" -> m.oc();
            case "MD" -> m.md();
            case "MC" -> m.mc();
            case "FD" -> m.md().subtract(horsFlux(jeu, compte, true));
            case "FC" -> m.mc().subtract(horsFlux(jeu, compte, false));
            default -> throw new IllegalArgumentException("Mesure inconnue : " + mesure);
        };
    }

    private static BigDecimal marquees(JeuMesures jeu, String compte, String mesure, String nature) {
        boolean flux = mesure.startsWith("F");
        boolean debit = mesure.endsWith("D");
        BigDecimal total = BigDecimal.ZERO;
        for (Marquee m : jeu.marquees()) {
            if (m.compte().equals(compte) && nature.equals(m.nature()) && !(flux && m.horsFlux())) {
                total = total.add(debit ? m.md() : m.mc());
            }
        }
        return total;
    }

    private static BigDecimal horsFlux(JeuMesures jeu, String compte, boolean debit) {
        BigDecimal total = BigDecimal.ZERO;
        for (Marquee m : jeu.marquees()) {
            if (m.compte().equals(compte) && m.horsFlux()) {
                total = total.add(debit ? m.md() : m.mc());
            }
        }
        return total;
    }

    /** Une ligne vide ne compte pas. Si toutes les lignes sont sans valeur, la somme non plus n'en a pas. */
    private BigDecimal somme(List<String> lignes, String colonne, int decalage) {
        BigDecimal total = null;
        for (String ligne : lignes) {
            Forme forme = forme(ligne, colonne);
            if (forme == null || forme.vide()) {
                continue;
            }
            BigDecimal valeur = cellule(ligne, colonne, decalage);
            if (valeur != null) {
                total = total == null ? valeur : total.add(valeur);
            }
        }
        return total;
    }

    private BigDecimal expression(String texte, int decalage) {
        List<Jeton> jetons = jetons(texte);
        return addition(jetons, new int[]{0}, decalage);
    }

    private BigDecimal addition(List<Jeton> jetons, int[] position, int decalage) {
        BigDecimal valeur = multiplication(jetons, position, decalage);
        while (position[0] < jetons.size() && jetons.get(position[0]).estOperateur("+", "-")) {
            String operateur = jetons.get(position[0]++).texte();
            BigDecimal droite = multiplication(jetons, position, decalage);
            valeur = valeur == null || droite == null ? null : "+".equals(operateur) ? valeur.add(droite) : valeur.subtract(droite);
        }
        return valeur;
    }

    private BigDecimal multiplication(List<Jeton> jetons, int[] position, int decalage) {
        BigDecimal valeur = unaire(jetons, position, decalage);
        while (position[0] < jetons.size() && jetons.get(position[0]).estOperateur("*", "/")) {
            String operateur = jetons.get(position[0]++).texte();
            BigDecimal droite = unaire(jetons, position, decalage);
            if (valeur == null || droite == null) {
                valeur = null;
            } else if ("*".equals(operateur)) {
                valeur = valeur.multiply(droite);
            } else {
                valeur = droite.signum() == 0 ? BigDecimal.ZERO : valeur.divide(droite, MathContext.DECIMAL64);
            }
        }
        return valeur;
    }

    private BigDecimal unaire(List<Jeton> jetons, int[] position, int decalage) {
        Jeton jeton = jetons.get(position[0]);
        if (jeton.estOperateur("-")) {
            position[0]++;
            BigDecimal valeur = unaire(jetons, position, decalage);
            return valeur == null ? null : valeur.negate();
        }
        if (jeton.estOperateur("+")) {
            position[0]++;
            return unaire(jetons, position, decalage);
        }
        if (jeton.estOperateur("(")) {
            position[0]++;
            BigDecimal valeur = addition(jetons, position, decalage);
            position[0]++;
            return valeur;
        }
        position[0]++;
        if ("nombre".equals(jeton.genre())) {
            return new BigDecimal(jeton.texte());
        }
        if (jeton.colonne() != null) {
            Forme forme = forme(jeton.texte(), jeton.colonne());
            // Une colonne non servie vaut zéro dans un calcul ; une colonne d'exercice absent rend tout le calcul sans valeur.
            return forme == null || forme.vide() ? BigDecimal.ZERO : cellule(jeton.texte(), jeton.colonne(), decalage);
        }
        return fonction(jeton.texte(), jetons, position, decalage);
    }

    private BigDecimal fonction(String nom, List<Jeton> jetons, int[] position, int decalage) {
        position[0]++;
        if ("positif".equals(nom) || "negatif".equals(nom)) {
            BigDecimal valeur = addition(jetons, position, decalage);
            position[0]++;
            if (valeur == null) {
                return null;
            }
            return "positif".equals(nom) ? valeur.max(BigDecimal.ZERO) : valeur.negate().max(BigDecimal.ZERO);
        }
        if ("RATIO".equals(nom)) {
            BigDecimal a = addition(jetons, position, decalage);
            position[0]++;
            BigDecimal b = addition(jetons, position, decalage);
            position[0]++;
            return a == null || b == null || b.signum() == 0 ? null : a.divide(b, 6, java.math.RoundingMode.HALF_UP);
        }
        if (("BLOC".equals(nom) || "SOMME".equals(nom)) && saisies != null && decalage == 0) {
            StringBuilder cle = new StringBuilder();
            while (!jetons.get(position[0]).estOperateur(")")) {
                Jeton j = jetons.get(position[0]++);
                cle.append(j.estOperateur(",") ? "|" : j.texte());
            }
            position[0]++;
            String clef = cle.toString();
            return saisies.blocs().getOrDefault(clef, BigDecimal.ZERO);
        }
        // Une fonction que le module ne sert pas encore ne donne pas de valeur, plutôt qu'un zéro qui paraîtrait juste.
        int profondeur = 1;
        while (profondeur > 0) {
            Jeton courant = jetons.get(position[0]++);
            if (courant.estOperateur("(")) {
                profondeur++;
            } else if (courant.estOperateur(")")) {
                profondeur--;
            }
        }
        return null;
    }

    private record Jeton(String genre, String texte, String colonne) {
        boolean estOperateur(String... operateurs) {
            if (!"op".equals(genre)) {
                return false;
            }
            for (String o : operateurs) {
                if (o.equals(texte)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static List<Jeton> jetons(String texte) {
        List<Jeton> jetons = new ArrayList<>();
        Matcher m = JETON.matcher(texte);
        int position = 0;
        while (position < texte.length()) {
            if (!m.find(position) || m.start() != position || m.end() == position) {
                if (texte.substring(position).isBlank()) {
                    break;
                }
                throw new IllegalArgumentException("Formule illisible : " + texte);
            }
            position = m.end();
            if (m.group(1) != null) {
                jetons.add(new Jeton("nombre", m.group(1), null));
            } else if (m.group(2) != null) {
                String colonne = m.group(3);
                jetons.add(new Jeton("ref", m.group(2), colonne == null ? null : colonne.substring(1, colonne.length() - 1)));
            } else {
                jetons.add(new Jeton("op", m.group(4), null));
            }
        }
        return jetons;
    }
}
