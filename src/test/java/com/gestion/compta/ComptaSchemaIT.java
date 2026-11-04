package com.gestion.compta;

import com.gestion.compta.essais.EssaisInvariants;
import com.gestion.compta.essais.EssaisInvariants2;
import com.gestion.compta.essais.EssaisInvariants3;
import com.gestion.compta.essais.EssaisScenarios;
import com.gestion.compta.support.AbstractComptaIT;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les invariants de la base, essayés en SQL direct : ce que la base refuse quel que soit le code. Les essais commitent et posent leurs propres données,
 * donc chacun a sa base, créée par la vraie chaîne Liquibase. Une ligne « KO » échoue le test, et le nombre d'essais joués est vérifié.
 */
class ComptaSchemaIT extends AbstractComptaIT {

    private interface Essai {
        void jouer(String[] arguments) throws Exception;
    }

    private static void jouer(String nom, int attendus, Essai essai) throws Exception {
        String url = baseSeparee(nom);
        PrintStream sortie = System.out;
        ByteArrayOutputStream lu = new ByteArrayOutputStream();
        try (PrintStream capture = new PrintStream(lu, true, StandardCharsets.UTF_8)) {
            System.setOut(capture);
            essai.jouer(new String[] {url});
        } finally {
            System.setOut(sortie);
        }
        List<String> lignes = lu.toString(StandardCharsets.UTF_8).lines().toList();
        List<String> echecs = lignes.stream().filter(l -> l.startsWith("KO")).toList();
        long reussis = lignes.stream().filter(l -> l.startsWith("OK")).count();
        assertTrue(echecs.isEmpty(), () -> echecs.size() + " échec(s) sur " + (reussis + echecs.size()) + " :\n" + String.join("\n", echecs));
        assertEquals(attendus, reussis, "nombre d'essais joués");
    }

    @Test
    void lesInvariantsDuJournal() throws Exception {
        jouer("essais_invariants", 61, EssaisInvariants::main);
    }

    @Test
    void lesInvariantsDuCycleComplet() throws Exception {
        jouer("essais_cycle", 43, EssaisInvariants2::main);
    }

    @Test
    void lesInvariantsDeLaCloture() throws Exception {
        jouer("essais_cloture", 22, EssaisInvariants3::main);
    }

    @Test
    void lesScenariosDeLaPasserelleEtDeLaTva() throws Exception {
        jouer("essais_scenarios", 73, EssaisScenarios::main);
    }
}
