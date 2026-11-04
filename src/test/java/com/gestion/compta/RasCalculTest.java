package com.gestion.compta;

import com.gestion.service.compta.RasCalcul;
import com.gestion.service.compta.RasCalcul.Bareme;
import com.gestion.service.compta.RasCalcul.Entree;
import com.gestion.service.compta.RasCalcul.Resultat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("compta")
class RasCalculTest {

    private static final Bareme BAREME = new Bareme(new BigDecimal("75"), new BigDecimal("100"), new BigDecimal("5000"), new BigDecimal("50000"));
    private static final BigDecimal VINGT = new BigDecimal("20");

    private static Resultat r(String nature, Boolean physique, boolean attestation, String ttc, String cumul) {
        return RasCalcul.calculer(new Entree(nature, physique, attestation, false, new BigDecimal(ttc), VINGT, new BigDecimal(cumul), BAREME));
    }

    private static BigDecimal m(String s) {
        return new BigDecimal(s);
    }

    /** RAS-07, NC 735 p. 25-28 : dix lignes, 111 187,50 DH à verser. */
    @Test
    void lesDixLignesOfficielles() {
        Resultat[] lignes = {
                r("SERVICE", true, false, "3500", "3500"),
                r("SERVICE", true, false, "30000", "33500"),
                r("SERVICE", true, false, "4500", "38000"),
                r("SERVICE", true, true, "3500", "3500"),
                r("SERVICE", true, true, "45000", "48500"),
                r("SERVICE", true, true, "4500", "53000"),
                r("SERVICE", false, false, "15000", "15000"),
                r("BIEN_EQUIPEMENT", false, false, "600000", "15000"),
                r("MARCHANDISE", false, false, "30000", "15000"),
                r("TRAVAUX", false, true, "300000", "0"),
        };
        String[] attendus = {"0.00", "5000.00", "0.00", "0.00", "5625.00", "562.50", "0.00", "100000.00", "0.00", "0.00"};
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < lignes.length; i++) {
            assertEquals(m(attendus[i]), lignes[i].retenue(), "ligne " + (i + 1));
            total = total.add(lignes[i].retenue());
        }
        assertEquals(m("111187.50"), total);
        assertEquals(m("111188.00"), RasCalcul.versement(total));
        assertEquals(m("25000.00"), m("30000").subtract(lignes[1].retenue()));
        assertEquals(m("39375.00"), m("45000").subtract(lignes[4].retenue()));
        assertEquals(m("3937.50"), m("4500").subtract(lignes[5].retenue()));
        assertEquals(m("500000.00"), m("600000").subtract(lignes[7].retenue()));
    }

    @Test
    void tauxEtArticles() {
        Resultat service = r("SERVICE", true, false, "30000", "33500");
        assertEquals(m("100"), service.tauxRetenue());
        assertEquals("117-V-b", service.article());
        assertEquals(m("25000.00"), service.ht());
        assertEquals(m("5000.00"), service.tva());
        assertEquals(m("75"), r("SERVICE", true, true, "45000", "48500").tauxRetenue());
        assertEquals("117-IV", r("BIEN_EQUIPEMENT", false, false, "600000", "0").article());
    }

    @Test
    void lePaiementPartielNeRetientQueSurCeReglement() {
        assertEquals(m("1000.00"), r("BIEN_EQUIPEMENT", false, false, "6000", "0").retenue());
        assertEquals(m("2000.00"), r("BIEN_EQUIPEMENT", false, false, "12000", "0").retenue());
    }

    @Test
    void personneMoraleAuDessusDuSeuil() {
        Resultat r = RasCalcul.calculer(new Entree("SERVICE", false, false, true, m("15000"), VINGT, m("15000"), BAREME));
        assertEquals(m("2500.00"), r.retenue());
        assertEquals("117-V-c", r.article());
        Resultat avec = RasCalcul.calculer(new Entree("SERVICE", false, true, true, m("15000"), VINGT, m("15000"), BAREME));
        assertEquals(m("1875.00"), avec.retenue());
    }

    @Test
    void rienNEstSuppose() {
        assertTrue(RasCalcul.calculer(new Entree(null, true, false, false, m("1000"), VINGT, m("1000"), BAREME)).aQualifier());
        assertTrue(RasCalcul.calculer(new Entree("SERVICE", null, false, false, m("1000"), VINGT, m("1000"), BAREME)).aQualifier());
        assertFalse(r("MARCHANDISE", null, false, "1000", "0").aQualifier());
        assertFalse(r("SERVICE_EXCLU", true, false, "90000", "90000").aRetenir());
    }
}
