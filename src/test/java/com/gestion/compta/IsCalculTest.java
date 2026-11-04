package com.gestion.compta;

import com.gestion.service.compta.IsCalcul;
import com.gestion.service.compta.IsCalcul.Entree;
import com.gestion.service.compta.IsCalcul.Resultat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("compta")
class IsCalculTest {

    private static BigDecimal m(String v) {
        return new BigDecimal(v);
    }

    private static void egal(String attendu, BigDecimal valeur) {
        assertEquals(0, m(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private static Resultat calcul(String benefice, String taux, String ca, String acomptes) {
        return IsCalcul.calculer(new Entree(m(benefice), m(taux), m(ca), m("0.25"), m("3000"), false, m(acomptes), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void societeZDeLaNoteCirculaire733() {
        // BN 2022 de 250 000 : IS théorique 2023 de 31 250, acompte de 7 813
        egal("31250", IsCalcul.pourcent(m("250000"), m("12.5")));
        egal("7813", IsCalcul.acompte(m("250000"), m("12.5"), m("25")));
        // BN 2023 de 280 000, CA 1 200 000 : CM 3 000, IS 35 000, reliquat 3 750
        Resultat z2023 = calcul("280000", "12.5", "1200000", "31250");
        egal("3000", z2023.cm());
        egal("35000", z2023.isTheorique());
        egal("35000", z2023.impotDu());
        egal("3750", z2023.reliquat());
        // IS théorique 2024 de 42 000, acompte de 10 500
        egal("42000", IsCalcul.pourcent(m("280000"), m("15")));
        egal("10500", IsCalcul.acompte(m("280000"), m("15"), m("25")));
        // BN 2024 de 500 000, CA 3 000 000 : CM 7 500, IS 100 000, reliquat 58 000
        Resultat z2024 = calcul("500000", "20", "3000000", "42000");
        egal("7500", z2024.cm());
        egal("100000", z2024.impotDu());
        egal("58000", z2024.reliquat());
    }

    @Test
    void societeXDeLaNoteCirculaire733() {
        egal("8475000", IsCalcul.pourcent(m("30000000"), m("28.25")));
        egal("2118750", IsCalcul.acompte(m("30000000"), m("28.25"), m("25")));
        Resultat x2023 = calcul("40000000", "28.25", "160000000", "8475000");
        egal("400000", x2023.cm());
        egal("11300000", x2023.impotDu());
        egal("2825000", x2023.reliquat());
    }

    @Test
    void lesTranchesSuiventLeBeneficeEtNeSontPasProgressives() {
        assertEquals("BN_JUSQUA_300K", IsCalcul.cleDeTaux("GENERAL", m("300000")));
        assertEquals("BN_300K_A_1M", IsCalcul.cleDeTaux("GENERAL", m("300000.01")));
        assertEquals("BN_1M_A_100M", IsCalcul.cleDeTaux("GENERAL", m("1000000")));
        assertEquals("BN_100M_ET_PLUS", IsCalcul.cleDeTaux("GENERAL", m("100000000")));
        assertEquals("BANQUES_ASSURANCES", IsCalcul.cleDeTaux("BANQUES_ASSURANCES", m("10")));
    }

    @Test
    void laBaseSArrondiALaDizaineSuperieure() {
        egal("530000", IsCalcul.dizaineSuperieure(m("530000")));
        egal("530010", IsCalcul.dizaineSuperieure(m("530000.01")));
        egal("530010", IsCalcul.dizaineSuperieure(m("530001")));
    }

    @Test
    void laCotisationMinimaleSApplique() {
        Resultat petit = calcul("1000", "20", "100000", "0");
        egal("3000", petit.cm());
        egal("3000", petit.impotDu());
        Resultat perte = calcul("-5000", "20", "2000000", "0");
        egal("0", perte.baseArrondie());
        egal("0", perte.isTheorique());
        egal("5000", perte.impotDu());
    }

    @Test
    void unExcedentSeReporteSansCredit() {
        Resultat r = calcul("100000", "20", "1000000", "30000");
        egal("20000", r.impotDu());
        egal("0", r.reliquat());
        egal("10000", r.excedent());
    }

    @Test
    void lExonerationDeCmSuitTrenteSixEtSoixanteMois() {
        LocalDate fin = LocalDate.parse("2026-12-31");
        assertTrue(IsCalcul.exonereDeCm(LocalDate.parse("2024-01-01"), LocalDate.parse("2024-06-01"), fin));
        assertFalse(IsCalcul.exonereDeCm(LocalDate.parse("2021-01-01"), LocalDate.parse("2024-06-01"), fin), "au-delà de soixante mois depuis la constitution");
        assertFalse(IsCalcul.exonereDeCm(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-06-01"), fin));
        assertFalse(IsCalcul.exonereDeCm(null, null, fin));
        Resultat r = IsCalcul.calculer(new Entree(m("1000"), m("20"), m("100000"), m("0.25"), m("3000"), true, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        egal("0", r.cm());
        egal("200", r.impotDu());
    }

    @Test
    void lesAcomptesSuiventLOuvertureDeLExercice() {
        assertEquals(LocalDate.parse("2026-03-31"), IsCalcul.echeanceAcompte(LocalDate.parse("2026-01-01"), 1));
        assertEquals(LocalDate.parse("2026-12-31"), IsCalcul.echeanceAcompte(LocalDate.parse("2026-01-01"), 4));
        assertEquals(LocalDate.parse("2026-09-30"), IsCalcul.echeanceAcompte(LocalDate.parse("2026-07-01"), 1));
    }

    @Test
    void laContributionDeSolidariteCommenceAUnMillion() {
        assertNull(IsCalcul.cleCss(m("999999.99")));
        assertEquals("T1", IsCalcul.cleCss(m("1000000")));
        assertEquals("T2", IsCalcul.cleCss(m("5000000")));
        assertEquals("T3", IsCalcul.cleCss(m("10000000")));
        assertEquals("T4", IsCalcul.cleCss(m("40000000")));
    }
}
