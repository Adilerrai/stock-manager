package com.gestion.compta;

import com.gestion.service.compta.DelaisCalcul;
import com.gestion.service.compta.DelaisCalcul.Entree;
import com.gestion.service.compta.DelaisCalcul.Paiement;
import com.gestion.service.compta.DelaisCalcul.Resultat;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Tag("compta")
class DelaisCalculTest {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal TROIS = new BigDecimal("3.00");
    private static final BigDecimal SUIVANT = new BigDecimal("0.85");

    private static LocalDate d(String s) {
        return LocalDate.parse(s);
    }

    private static BigDecimal m(String s) {
        return new BigDecimal(s);
    }

    private static Entree facture(String ttc, String echeance, Paiement... paiements) {
        return new Entree(m(ttc), d(echeance), List.of(paiements), ZERO, ZERO, TROIS, SUIVANT);
    }

    @Test
    void echeancesOfficiellesDeLaNc734() {
        assertEquals(d("2023-09-14"), DelaisCalcul.echeanceCommerciale(d("2023-07-17"), 60, Set.of()));
        assertEquals(d("2023-11-20"), DelaisCalcul.echeanceCommerciale(d("2023-08-23"), 90, Set.of()));
    }

    @Test
    void lesDeuxDecomptesDifferentDUnJour() {
        assertEquals(d("2023-09-14"), DelaisCalcul.echeanceCommerciale(d("2023-07-17"), 60, Set.of()));
        assertEquals(d("2023-09-15"), DelaisCalcul.delaiFiscal(d("2023-07-17"), 60, Set.of()));
    }

    @Test
    void reportAuPremierJourOuvrableSeulementDepuisLaListe() {
        assertEquals(d("2023-09-14"), DelaisCalcul.reporter(d("2023-09-14"), Set.of()));
        assertEquals(d("2023-09-16"), DelaisCalcul.reporter(d("2023-09-14"), Set.of(d("2023-09-14"), d("2023-09-15"))));
        assertEquals(d("2023-09-15"), DelaisCalcul.echeanceCommerciale(d("2023-07-17"), 60, Set.of(d("2023-09-14"))));
    }

    @Test
    void factureEmiseApresLeMoisDeLivraison() {
        assertEquals(d("2023-07-31"), DelaisCalcul.depart(d("2023-08-10"), d("2023-07-05")));
        assertEquals(d("2023-07-20"), DelaisCalcul.depart(d("2023-07-20"), d("2023-07-05")));
        assertEquals(d("2023-07-20"), DelaisCalcul.depart(d("2023-07-20"), null));
    }

    /** NC 734 p. 11-12 : T3 2023 = 120 000 DH, T4 2023 = 299 000 DH. */
    @Test
    void lesDeuxTrimestresOfficiels() {
        Entree quatreMillions = facture("4000000.00", "2023-09-14", new Paiement(d("2023-11-10"), m("4000000.00")));
        Entree sixMillions = facture("6000000.00", "2023-11-20", new Paiement(d("2023-12-10"), m("6000000.00")));

        Resultat t3 = DelaisCalcul.calculer(quatreMillions, d("2023-07-01"), d("2023-09-30"));
        assertEquals(m("120000.00"), t3.amende());
        assertEquals(1, t3.mois());
        assertEquals(m("4000000.00"), t3.nonPaye());

        Resultat a = DelaisCalcul.calculer(quatreMillions, d("2023-10-01"), d("2023-12-31"));
        assertEquals(m("68000.00"), a.amende());
        assertEquals(2, a.mois());
        assertEquals(m("1.70"), a.taux());
        assertEquals(ZERO, a.nonPaye());
        assertEquals(m("4000000.00"), a.horsDelai());
        assertEquals(d("2023-11-10"), a.dernierPaiementHorsDelai());

        Resultat b = DelaisCalcul.calculer(sixMillions, d("2023-10-01"), d("2023-12-31"));
        assertEquals(m("231000.00"), b.amende());
        assertEquals(2, b.mois());
        assertEquals(m("3.85"), b.taux());

        assertEquals(m("299000.00"), a.amende().add(b.amende()));
        assertFalse(DelaisCalcul.calculer(sixMillions, d("2023-07-01"), d("2023-09-30")).aDeclarer());
    }

    @Test
    void tauxDuMoisOuCommenceLeRetard() {
        assertEquals(d("2023-09-30"), DelaisCalcul.dateDuTaux(d("2023-09-14")));
        assertEquals(d("2023-10-31"), DelaisCalcul.dateDuTaux(d("2023-09-30")));
    }

    @Test
    void echeanceAuDernierJourDuMois() {
        Resultat t3 = DelaisCalcul.calculer(facture("1000.00", "2023-09-30"), d("2023-07-01"), d("2023-09-30"));
        assertFalse(t3.aDeclarer());
        Resultat t4 = DelaisCalcul.calculer(facture("1000.00", "2023-09-30"), d("2023-10-01"), d("2023-12-31"));
        assertEquals(3, t4.mois());
        assertEquals(m("47.00"), t4.amende());
    }

    @Test
    void paiementDansLeDelaiSansAmende() {
        Resultat r = DelaisCalcul.calculer(facture("1000.00", "2023-09-14", new Paiement(d("2023-09-14"), m("1000.00"))), d("2023-07-01"), d("2023-12-31"));
        assertFalse(r.aDeclarer());
    }

    /** La règle du paiement partiel n'est pas confirmée (DP-13) : chaque tranche paie ses mois. */
    @Test
    void paiementPartiel() {
        Entree e = facture("1000.00", "2023-09-14", new Paiement(d("2023-09-10"), m("200.00")), new Paiement(d("2023-10-05"), m("300.00")));
        Resultat r = DelaisCalcul.calculer(e, d("2023-10-01"), d("2023-12-31"));
        assertEquals(m("500.00"), r.nonPaye());
        assertEquals(m("300.00"), r.horsDelai());
        assertEquals(m("15.30"), r.amende());
    }

    @Test
    void litigeSuspenduPuisRattrapeAuJugement() {
        Entree suspendu = new Entree(m("1000.00"), d("2023-09-14"), List.of(), m("1000.00"), ZERO, TROIS, SUIVANT);
        assertEquals(ZERO, DelaisCalcul.calculer(suspendu, d("2023-10-01"), d("2023-12-31")).amende());

        Entree juge = new Entree(m("1000.00"), d("2023-09-14"), List.of(), ZERO, m("1000.00"), TROIS, SUIVANT);
        Resultat r = DelaisCalcul.calculer(juge, d("2023-10-01"), d("2023-12-31"));
        assertEquals(m("55.50"), r.amende());
        assertEquals(4, r.mois());
    }

    @Test
    void applicationSelonLeChiffreDAffaires() {
        assertEquals(d("2023-07-01"), DelaisCalcul.applicableDepuis(m("60000000")));
        assertEquals(d("2024-01-01"), DelaisCalcul.applicableDepuis(m("50000000")));
        assertEquals(d("2025-01-01"), DelaisCalcul.applicableDepuis(m("10000000")));
    }

    @Test
    void versementAuDirhamSuperieur() {
        assertEquals(m("121.00"), DelaisCalcul.versement(m("120.01")));
        assertEquals(m("120.00"), DelaisCalcul.versement(m("120.00")));
    }
}
