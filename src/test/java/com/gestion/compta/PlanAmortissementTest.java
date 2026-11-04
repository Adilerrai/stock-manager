package com.gestion.compta;

import com.gestion.service.compta.PlanAmortissement;
import com.gestion.service.compta.PlanAmortissement.Annuite;
import com.gestion.service.compta.PlanAmortissement.Fiche;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("compta")
class PlanAmortissementTest {

    private static final BigDecimal BRUT = new BigDecimal("100000.00");
    private static final LocalDate JANVIER = LocalDate.parse("2024-01-01");
    private static final LocalDate JUILLET = LocalDate.parse("2024-07-01");

    private static void egal(String attendu, BigDecimal valeur) {
        assertEquals(0, new BigDecimal(attendu).compareTo(valeur), "attendu " + attendu + ", obtenu " + valeur);
    }

    private static Fiche lineaire(int annees, LocalDate service) {
        return new Fiche(BRUT, BigDecimal.ZERO, "LINEAIRE", annees * 12, null, service);
    }

    private static BigDecimal total(List<Annuite> plan) {
        return plan.stream().map(Annuite::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<Annuite> plan(Fiche f) {
        return PlanAmortissement.prevision(f, BigDecimal.ZERO, LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"));
    }

    @Test
    void s12LaValeurNetteArriveExactementAZeroPourChaqueDuree() {
        for (int annees : List.of(3, 4, 5, 6, 7, 8, 9, 10, 12, 15)) {
            egal("100000.00", total(plan(lineaire(annees, JANVIER))));
            egal("100000.00", total(plan(lineaire(annees, JUILLET))));
            assertEquals(annees, plan(lineaire(annees, JANVIER)).size(), "une annuité par exercice");
            assertEquals(annees + 1, plan(lineaire(annees, JUILLET)).size(), "une annuité de plus quand le service commence en juillet");
        }
    }

    @Test
    void s12LaDerniereAnnuiteAbsorbeLArrondi() {
        List<Annuite> p = plan(lineaire(3, JANVIER));

        egal("33333.33", p.get(0).montant());
        egal("33333.33", p.get(1).montant());
        egal("33333.34", p.get(2).montant());
    }

    @Test
    void leProrataSuitLesJoursDe30Sur360() {
        assertEquals(360, PlanAmortissement.jours360(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")));
        assertEquals(30, PlanAmortissement.jours360(LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28")));
        assertEquals(180, PlanAmortissement.jours360(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30")));
        assertEquals(180, PlanAmortissement.jours360(LocalDate.parse("2024-07-01"), LocalDate.parse("2024-12-31")));
        egal("16666.67", plan(lineaire(3, JUILLET)).get(0).montant());
    }

    @Test
    void s11LeComplementSuitLaDateDeSortie() {
        Fiche f = new Fiche(BRUT, new BigDecimal("40000.00"), "LINEAIRE", 60, null, JANVIER);

        egal("10000.00", PlanAmortissement.dotation(f, BigDecimal.ZERO, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-06-30")));
    }

    @Test
    void s12LeDegressifResteAuPlusEgalALaValeurBrute() {
        for (LocalDate service : List.of(JANVIER, JUILLET)) {
            Fiche f = new Fiche(BRUT, BigDecimal.ZERO, "DEGRESSIF", 36, new BigDecimal("1.5"), service);
            List<Annuite> p = plan(f);
            egal("100000.00", total(p));
            assertTrue(p.stream().allMatch(a -> a.montant().signum() > 0));
        }
        egal("50000.00", plan(new Fiche(BRUT, BigDecimal.ZERO, "DEGRESSIF", 36, new BigDecimal("1.5"), JANVIER)).get(0).montant());
        egal("25000.00", plan(new Fiche(BRUT, BigDecimal.ZERO, "DEGRESSIF", 36, new BigDecimal("1.5"), JANVIER)).get(1).montant());
        egal("25000.00", plan(new Fiche(BRUT, BigDecimal.ZERO, "DEGRESSIF", 36, new BigDecimal("1.5"), JANVIER)).get(2).montant());
    }

    @Test
    void uneRepriseReduitCeQuiResteADoter() {
        Fiche f = new Fiche(BRUT, new BigDecimal("90000.00"), "LINEAIRE", 60, null, JANVIER);

        egal("10000.00", PlanAmortissement.dotation(f, BigDecimal.ZERO, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")));
        egal("0", PlanAmortissement.dotation(f, new BigDecimal("10000.00"), LocalDate.parse("2027-01-01"), LocalDate.parse("2027-12-31")));
    }

    @Test
    void unBienSansServiceOuNonAmortissableNeSeDotePas() {
        egal("0", PlanAmortissement.dotation(new Fiche(BRUT, BigDecimal.ZERO, "LINEAIRE", 60, null, null), BigDecimal.ZERO, JANVIER, LocalDate.parse("2024-12-31")));
        egal("0", PlanAmortissement.dotation(new Fiche(BRUT, BigDecimal.ZERO, "NON_AMORTISSABLE", 0, null, JANVIER), BigDecimal.ZERO, JANVIER, LocalDate.parse("2024-12-31")));
        egal("0", PlanAmortissement.dotation(lineaire(5, LocalDate.parse("2025-03-01")), BigDecimal.ZERO, JANVIER, LocalDate.parse("2024-12-31")));
    }

    @Test
    void leCasOfficielDeReversementDonneCentCinquanteMille() {
        egal("150000.00", PlanAmortissement.reversementTva(new BigDecimal("200000.00"), LocalDate.parse("2025-01-01"), LocalDate.parse("2026-04-01")));
        egal("0", PlanAmortissement.reversementTva(new BigDecimal("200000.00"), LocalDate.parse("2020-01-01"), LocalDate.parse("2026-04-01")));
    }
}
