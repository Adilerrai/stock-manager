package com.gestion.compta;

import com.gestion.service.compta.DefinitionsEtats;
import com.gestion.service.compta.DefinitionsEtats.Controle;
import com.gestion.service.compta.DefinitionsEtats.Regle;
import com.gestion.service.compta.MoteurEtats;
import com.gestion.service.compta.MoteurEtats.JeuMesures;
import com.gestion.service.compta.MoteurEtats.Marquee;
import com.gestion.service.compta.MoteurEtats.MesureCompte;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@Tag("compta")
class MoteurEtatsTest {

    private static final DefinitionsEtats D = DefinitionsEtats.instance();
    private static final BigDecimal Z = BigDecimal.ZERO;
    private static final BigDecimal CENT = new BigDecimal("100.00");
    /**
     * Hors du tirage sur des comptes quelconques. Dotations marquées et tableau T14 demandent une donnée absente des mesures simulées.
     * Le tableau de financement ne s'équilibre que sur des flux réels (une dotation vers 28, un capital marqué hors flux...) : voir l'essai suivant.
     */
    private static final Set<String> HORS_SIMULATION = Set.of("CAF_DOTATIONS", "DISTRIBUTION_T14", "TF_TOTAUX_EGAUX", "TF_VARIATION_FRF");

    @Test
    void chaqueCompteTombeSurSaLigneDansLesDeuxSens() throws IOException {
        List<String> fautes = new ArrayList<>();
        List<String[]> cas = cas();
        assertEquals(1576, cas.size());
        for (String[] c : cas) {
            String compte = c[0];
            char sens = c[1].charAt(0);
            BigDecimal attendu = CENT.multiply(new BigDecimal(c[5]));
            MoteurEtats moteur = moteur(mesures(Map.of(compte, sens == 'D' ? solde(CENT, Z) : solde(Z, CENT))));
            BigDecimal valeur = moteur.cellule(c[3], c[4]);
            if (valeur == null || valeur.compareTo(attendu) != 0) {
                fautes.add(compte + " " + sens + " " + c[3] + "/" + c[4] + " : " + valeur + " au lieu de " + attendu);
            }
            Regle regle = D.regle(compte, sens);
            if (regle == null || !regle.ligne().equals(c[3]) || !regle.colonne().equals(c[4]) || regle.signe() != Integer.parseInt(c[5])
                    || regle.reclassement() != "reclassement".equals(c.length > 6 ? c[6] : "")) {
                fautes.add(compte + " " + sens + " : règle " + regle);
            }
        }
        assertEquals(List.of(), fautes);
    }

    /** S2 : capital 10 000, achats 30 000, banque créditrice de 20 000. Aucun montant ne disparaît et le bilan tient. */
    @Test
    void unDecouvertPasseAuPassifSansRienPerdre() {
        MoteurEtats m = moteur(mesures(Map.of("1111", solde(Z, bd("10000")), "6111", solde(bd("30000"), Z), "5141", solde(Z, bd("20000")))));

        egal("0", m.cellule("ACT.514", "NET"));
        egal("20000", m.cellule("PAS.554", "NET"));
        egal("-30000", m.cellule("CPC.XVI", "TOTAUX"));
        egal("0", m.cellule("ACT.TOTAL_GENERAL", "NET"));
        egal("0", m.cellule("PAS.TOTAL_GENERAL", "NET"));
    }

    @Test
    void lesMesuresDeTiersNeSeCompensentPas() {
        // Déjà sommées tiers par tiers : un client débiteur de 1 000 et un autre créditeur de 300
        MoteurEtats m = moteur(mesures(Map.of("3421", solde(bd("1000"), bd("300")), "1111", solde(Z, bd("700")))));

        egal("1000", m.cellule("ACT.342", "BRUT"));
        egal("300", m.cellule("PAS.442", "NET"));
        egal("1000", m.cellule("ACT.TOTAL_GENERAL", "NET"));
        egal("1000", m.cellule("PAS.TOTAL_GENERAL", "NET"));
    }

    @Test
    void sansExercicePrecedentLaColonneResteVideEtNeVautPasZero() {
        MoteurEtats m = moteur(mesures(Map.of("1111", solde(Z, bd("100")), "5141", solde(bd("100"), Z))));

        assertNull(m.cellule("ACT.514", "NET_N1"));
        assertNull(m.cellule("ACT.TOTAL_GENERAL", "NET_N1"));
        assertNull(m.expression("ACT.TOTAL_GENERAL[NET_N1]"));
        assertNull(m.cellule("TF1.1", "B"));
        assertNull(m.cellule("TF1.1", "RESSOURCES"));
        assertNotNull(m.cellule("ACT.514", "NET"));
    }

    @Test
    void lesColonnesPrecedentesLisentLExercicePrecedent() {
        JeuMesures n = mesures(Map.of("5141", solde(bd("500"), Z), "1111", solde(Z, bd("500"))));
        JeuMesures n1 = mesures(Map.of("5141", solde(bd("200"), Z), "1111", solde(Z, bd("200"))));
        MoteurEtats m = new MoteurEtats(D, List.of(n, n1));

        egal("500", m.cellule("ACT.514", "NET"));
        egal("200", m.cellule("ACT.514", "NET_N1"));
        egal("200", m.cellule("ACT.TOTAL_GENERAL", "NET_N1"));
        egal("300", m.cellule("TF1.7", "A").subtract(m.cellule("TF1.7", "B")));
    }

    @Test
    void uneNatureDeMouvementSeLitSurLesLignesMarquees() {
        Map<String, MesureCompte> comptes = new TreeMap<>();
        comptes.put("2340", new MesureCompte(Z, Z, bd("900"), bd("100"), bd("800"), Z));
        comptes.put("5141", new MesureCompte(Z, Z, bd("100"), bd("900"), Z, bd("800")));
        JeuMesures jeu = new JeuMesures(comptes, List.of(new Marquee("2340", "ACQUISITION", false, bd("900"), Z), new Marquee("2340", "VIREMENT", true, Z, bd("100"))));
        MoteurEtats m = moteur(jeu);

        DefinitionsEtats.Terme acquisition = new DefinitionsEtats.Terme("MD", List.of("23"), List.of(), 1, "ACQUISITION", false);
        DefinitionsEtats.Terme flux = new DefinitionsEtats.Terme("FC", List.of("23"), List.of(), 1, null, false);
        egal("900", m.termes(List.of(acquisition)));
        // MC moins ce qui est hors flux
        egal("0", m.termes(List.of(flux)));
    }

    @Test
    void leDetailDUneCelluleRetrouveSaValeur() {
        MoteurEtats m = moteur(mesures(Map.of("2351", solde(bd("1000"), Z), "28351", solde(Z, bd("400")), "1111", solde(Z, bd("600")))));

        List<MoteurEtats.Contribution> detail = m.contributions("ACT.235", "NET");
        BigDecimal somme = detail.stream().map(MoteurEtats.Contribution::montant).reduce(Z, BigDecimal::add);
        egal(m.cellule("ACT.235", "NET").toPlainString(), somme);
        assertFalse(detail.isEmpty());
        assertNotNull(m.formule("ACT.235", "BRUT"));
    }

    /** Exercices simulés et enchaînés : chaque contrôle de la définition doit tenir, quels que soient les comptes tirés. */
    @Test
    void lesControlesTiennentSurDesExercicesSimules() throws IOException {
        List<String> comptes = comptesImputables();
        List<String> fautes = new ArrayList<>();
        for (int tirage = 0; tirage < 200; tirage++) {
            Random hasard = new Random(tirage);
            Map<String, BigDecimal[]> anterieur = mouvements(hasard, comptes);
            JeuMesures n1 = clore(Map.of(), anterieur);
            Map<String, BigDecimal> ouverture = ouvertures(n1);
            JeuMesures n = clore(ouverture, mouvements(hasard, comptes));
            MoteurEtats m = new MoteurEtats(D, List.of(n, n1));
            for (Controle c : D.controles()) {
                if (c.gauche() == null || HORS_SIMULATION.contains(c.code())) {
                    continue;
                }
                BigDecimal gauche = m.expression(c.gauche());
                BigDecimal droite = c.droiteTermes() != null ? m.termes(c.droiteTermes()) : m.expression(c.droite());
                if (gauche == null || droite == null || gauche.compareTo(droite) != 0) {
                    fautes.add(tirage + " " + c.code() + " : " + gauche + " contre " + droite);
                }
            }
        }
        assertEquals(List.of(), fautes.subList(0, Math.min(fautes.size(), 20)), fautes.size() + " échecs");
    }

    /** Deux exercices aux flux réels : emplois et ressources s'égalent, et la variation du fonds de roulement se retrouve par les deux chemins. */
    @Test
    void leTableauDeFinancementSEquilibreSurDesFluxReels() {
        List<String> fautes = new ArrayList<>();
        for (int tirage = 0; tirage < 100; tirage++) {
            Random hasard = new Random(tirage);
            JeuMesures n1 = clore(Map.of(), fluxReels(hasard));
            JeuMesures n = clore(ouvertures(n1), fluxReels(hasard));
            MoteurEtats m = new MoteurEtats(D, List.of(n, n1));
            for (Controle c : D.controles()) {
                if (c.gauche() == null || !c.code().startsWith("TF_") && !c.code().startsWith("CAF_METHODE")) {
                    continue;
                }
                BigDecimal gauche = m.expression(c.gauche());
                BigDecimal droite = m.expression(c.droite());
                if (gauche == null || droite == null || gauche.compareTo(droite) != 0) {
                    fautes.add(tirage + " " + c.code() + " : " + gauche + " contre " + droite);
                }
            }
        }
        assertEquals(List.of(), fautes.subList(0, Math.min(fautes.size(), 10)), fautes.size() + " échecs");
    }

    private static Map<String, BigDecimal[]> fluxReels(Random h) {
        Map<String, BigDecimal[]> m = new HashMap<>();
        ecrire(m, "5141", "1111", montant(h, 200_000, 900_000));
        BigDecimal emprunt = montant(h, 50_000, 300_000);
        ecrire(m, "5141", "1481", emprunt);
        ecrire(m, "1481", "5141", emprunt.divide(BigDecimal.valueOf(4 + h.nextInt(6)), 2, java.math.RoundingMode.HALF_UP));
        BigDecimal vehicule = montant(h, 40_000, 200_000);
        ecrire(m, "2340", "5141", vehicule);
        ecrire(m, "61934", "2834", vehicule.divide(BigDecimal.valueOf(5), 2, java.math.RoundingMode.HALF_UP));
        BigDecimal achat = montant(h, 100_000, 400_000);
        ecrire(m, "6111", "4411", achat);
        ecrire(m, "34552", "4411", achat.divide(BigDecimal.valueOf(5), 2, java.math.RoundingMode.HALF_UP));
        ecrire(m, "4411", "5141", achat);
        BigDecimal vente = montant(h, 200_000, 700_000);
        ecrire(m, "3421", "7111", vente);
        ecrire(m, "3421", "4455", vente.divide(BigDecimal.valueOf(5), 2, java.math.RoundingMode.HALF_UP));
        ecrire(m, "5141", "3421", vente);
        ecrire(m, "61711", "5141", montant(h, 10_000, 90_000));
        return m;
    }

    private static BigDecimal montant(Random h, int minimum, int maximum) {
        return BigDecimal.valueOf(minimum + h.nextInt(maximum - minimum)).setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private static void ecrire(Map<String, BigDecimal[]> mouvements, String debit, String credit, BigDecimal montant) {
        BigDecimal[] d = mouvements.computeIfAbsent(debit, k -> new BigDecimal[]{Z, Z});
        d[0] = d[0].add(montant);
        BigDecimal[] c = mouvements.computeIfAbsent(credit, k -> new BigDecimal[]{Z, Z});
        c[1] = c[1].add(montant);
    }

    private static Map<String, BigDecimal[]> mouvements(Random hasard, List<String> comptes) {
        Map<String, BigDecimal[]> mouvements = new HashMap<>();
        for (int i = 0; i < 40; i++) {
            String debit = comptes.get(hasard.nextInt(comptes.size()));
            String credit = comptes.get(hasard.nextInt(comptes.size()));
            BigDecimal montant = BigDecimal.valueOf(1 + hasard.nextInt(5_000_000), 2);
            mouvements.computeIfAbsent(debit, k -> new BigDecimal[]{Z, Z})[0] = mouvements.get(debit)[0].add(montant);
            mouvements.computeIfAbsent(credit, k -> new BigDecimal[]{Z, Z})[1] = mouvements.get(credit)[1].add(montant);
        }
        return mouvements;
    }

    private static JeuMesures clore(Map<String, BigDecimal> ouverture, Map<String, BigDecimal[]> mouvements) {
        Map<String, MesureCompte> resultat = new TreeMap<>();
        for (String compte : union(ouverture, mouvements)) {
            BigDecimal o = ouverture.getOrDefault(compte, Z);
            BigDecimal[] mv = mouvements.getOrDefault(compte, new BigDecimal[]{Z, Z});
            BigDecimal solde = o.add(mv[0]).subtract(mv[1]);
            resultat.put(compte, new MesureCompte(o.max(Z), o.negate().max(Z), mv[0], mv[1], solde.max(Z), solde.negate().max(Z)));
        }
        return new JeuMesures(resultat, List.of());
    }

    private static List<String> union(Map<String, BigDecimal> a, Map<String, BigDecimal[]> b) {
        List<String> toutes = new ArrayList<>(a.keySet());
        b.keySet().stream().filter(k -> !a.containsKey(k)).forEach(toutes::add);
        Collections.sort(toutes);
        return toutes;
    }

    /** Classes 1 à 5 reportées, gestion soldée sur le résultat : ce que fera l'écriture d'à-nouveau. */
    private static Map<String, BigDecimal> ouvertures(JeuMesures cloture) {
        Map<String, BigDecimal> ouverture = new HashMap<>();
        BigDecimal resultat = Z;
        for (Map.Entry<String, MesureCompte> e : cloture.mesures().entrySet()) {
            BigDecimal solde = e.getValue().sd().subtract(e.getValue().sc());
            if (e.getKey().charAt(0) <= '5') {
                ouverture.put(e.getKey(), solde);
            } else {
                resultat = resultat.add(solde);
            }
        }
        if (resultat.signum() != 0) {
            ouverture.merge(resultat.signum() > 0 ? "1199" : "1191", resultat, BigDecimal::add);
        }
        return ouverture;
    }

    private static List<String> comptesImputables() throws IOException {
        List<String> comptes = new ArrayList<>();
        try (BufferedReader lecteur = lecteur("/compta/plan_cgnc.csv")) {
            lecteur.readLine();
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                String numero = ligne.split(";")[0];
                if (numero.length() >= 4 && numero.charAt(0) >= '1' && numero.charAt(0) <= '7' && D.regle(numero, 'D') != null && D.regle(numero, 'C') != null) {
                    comptes.add(numero);
                }
            }
        }
        return comptes;
    }

    private static List<String[]> cas() throws IOException {
        List<String[]> lignes = new ArrayList<>();
        try (BufferedReader lecteur = lecteur("/compta/affectation_resolue.csv")) {
            lecteur.readLine();
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                if (!ligne.isBlank()) {
                    lignes.add(ligne.split(";", -1));
                }
            }
        }
        return lignes;
    }

    private static BufferedReader lecteur(String ressource) {
        InputStream flux = MoteurEtatsTest.class.getResourceAsStream(ressource);
        assertNotNull(flux, ressource);
        return new BufferedReader(new InputStreamReader(flux, StandardCharsets.UTF_8));
    }

    private static MoteurEtats moteur(JeuMesures jeu) {
        return new MoteurEtats(D, Arrays.asList(jeu, null, null));
    }

    private static JeuMesures mesures(Map<String, MesureCompte> comptes) {
        return new JeuMesures(new TreeMap<>(comptes), List.of());
    }

    private static MesureCompte solde(BigDecimal sd, BigDecimal sc) {
        return new MesureCompte(Z, Z, sd, sc, sd, sc);
    }

    private static BigDecimal bd(String valeur) {
        return new BigDecimal(valeur);
    }

    private static void egal(String attendu, BigDecimal valeur) {
        assertNotNull(valeur);
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }
}
