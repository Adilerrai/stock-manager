package com.gestion.service.compta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Délais de paiement : le calcul seul, sans base. Deux fonctions de date, car les deux décomptes diffèrent :
 * le code de commerce compte la date d'émission comme premier jour, le CGI (art. 163-I) ne compte pas le premier jour.
 * Le mois de retard est un mois civil touché, règle déduite de l'exemple de la NC 734 p. 11-12 et donc à faire vérifier.
 */
public final class DelaisCalcul {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private DelaisCalcul() {
    }

    public record Paiement(LocalDate date, BigDecimal montant) {
    }

    /** {@code suspendu} : le litige devant le tribunal, sans jugement à la fin de la période. {@code rattrape} : le litige jugé dans la période, dont l'amende court depuis le début du retard. */
    public record Entree(BigDecimal ttc, LocalDate echeance, List<Paiement> paiements, BigDecimal suspendu, BigDecimal rattrape, BigDecimal tauxPremierMois, BigDecimal tauxMoisSuivant) {
    }

    public record Resultat(BigDecimal nonPaye, BigDecimal horsDelai, LocalDate dernierPaiementHorsDelai, int mois, BigDecimal taux, BigDecimal amende) {

        public boolean aDeclarer() {
            return amende.signum() > 0 || nonPaye.signum() > 0 || horsDelai.signum() > 0;
        }
    }

    /** DP-04 : le délai court de l'émission, ou de la fin du mois de livraison si la facture est émise après. */
    public static LocalDate depart(LocalDate emission, LocalDate livraison) {
        if (livraison == null) {
            return emission;
        }
        LocalDate finDuMois = YearMonth.from(livraison).atEndOfMonth();
        return emission.isAfter(finDuMois) ? finDuMois : emission;
    }

    /** DP-04 : la date de départ compte comme premier jour, puis report au premier jour ouvrable. */
    public static LocalDate echeanceCommerciale(LocalDate depart, int delai, Set<LocalDate> feries) {
        return reporter(depart.plusDays(delai - 1L), feries);
    }

    /** CGI art. 163-I : le premier jour ne compte pas, puis report au premier jour ouvrable. */
    public static LocalDate delaiFiscal(LocalDate depart, int jours, Set<LocalDate> feries) {
        return reporter(depart.plusDays(jours), feries);
    }

    /** Rien ne se reporte sans la liste : les jours chômés sont ceux de la table, aucun n'est supposé. */
    public static LocalDate reporter(LocalDate date, Set<LocalDate> feries) {
        LocalDate d = date;
        while (feries.contains(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** Le taux du premier mois est celui en vigueur le dernier jour du mois où commence le retard (DP-07). */
    public static LocalDate dateDuTaux(LocalDate echeance) {
        return YearMonth.from(echeance.plusDays(1)).atEndOfMonth();
    }

    /** § 10.5 : l'amende s'applique selon le chiffre d'affaires, aux factures émises depuis ces dates. */
    public static LocalDate applicableDepuis(BigDecimal chiffreAffaires) {
        if (chiffreAffaires.compareTo(BigDecimal.valueOf(50_000_000)) > 0) {
            return LocalDate.of(2023, 7, 1);
        }
        return chiffreAffaires.compareTo(BigDecimal.valueOf(10_000_000)) > 0 ? LocalDate.of(2024, 1, 1) : LocalDate.of(2025, 1, 1);
    }

    /**
     * L'amende d'une facture pour la période déclarée. Seuls les mois de la période se déclarent.
     * Un paiement partiel forme une tranche : la règle n'est pas confirmée par les textes (DP-13).
     */
    public static Resultat calculer(Entree e, LocalDate du, LocalDate au) {
        YearMonth premier = YearMonth.from(e.echeance().plusDays(1));
        YearMonth debut = YearMonth.from(du);
        YearMonth fin = YearMonth.from(au);
        List<Paiement> paiements = new ArrayList<>(e.paiements());
        paiements.sort(Comparator.comparing(Paiement::date));
        BigDecimal paye = Z;
        BigDecimal horsDelai = Z;
        BigDecimal amende = Z;
        LocalDate dernier = null;
        BigDecimal tauxTranche = Z;
        int moisTranche = 0;
        for (Paiement p : paiements) {
            if (p.date().isAfter(au)) {
                continue;
            }
            paye = paye.add(p.montant());
            if (!p.date().isAfter(e.echeance())) {
                continue;
            }
            YearMonth moisPaiement = YearMonth.from(p.date());
            if (moisPaiement.isBefore(debut)) {
                continue;
            }
            Tranche t = tranche(p.montant(), premier, moisPaiement, debut, e);
            horsDelai = horsDelai.add(p.montant());
            amende = amende.add(t.amende());
            dernier = p.date();
            if (t.mois() >= moisTranche) {
                moisTranche = t.mois();
                tauxTranche = t.taux();
            }
        }
        BigDecimal nonPaye = e.ttc().subtract(paye).max(Z);
        boolean enRetard = e.echeance().isBefore(au);
        if (enRetard && nonPaye.signum() > 0) {
            BigDecimal rattrape = e.rattrape().min(nonPaye);
            BigDecimal assiette = nonPaye.subtract(e.suspendu().min(nonPaye)).subtract(rattrape).max(Z);
            Tranche t = tranche(assiette, premier, fin, debut, e);
            amende = amende.add(t.amende());
            if (assiette.signum() > 0 && t.mois() >= moisTranche) {
                moisTranche = t.mois();
                tauxTranche = t.taux();
            }
            if (rattrape.signum() > 0) {
                Tranche r = tranche(rattrape, premier, fin, premier, e);
                amende = amende.add(r.amende());
                if (r.mois() >= moisTranche) {
                    moisTranche = r.mois();
                    tauxTranche = r.taux();
                }
            }
        }
        return new Resultat(enRetard ? nonPaye : Z, horsDelai, dernier, moisTranche, tauxTranche, amende.setScale(2, RoundingMode.HALF_UP));
    }

    private record Tranche(int mois, BigDecimal taux, BigDecimal amende) {
    }

    private static Tranche tranche(BigDecimal assiette, YearMonth premier, YearMonth dernier, YearMonth debut, Entree e) {
        int mois = 0;
        BigDecimal taux = BigDecimal.ZERO;
        for (YearMonth m = premier; !m.isAfter(dernier); m = m.plusMonths(1)) {
            if (m.isBefore(debut)) {
                continue;
            }
            mois++;
            taux = taux.add(m.equals(premier) ? e.tauxPremierMois() : e.tauxMoisSuivant());
        }
        return new Tranche(mois, taux, assiette.multiply(taux).divide(CENT, 2, RoundingMode.HALF_UP));
    }

    /** NC 734 p. 3 : le versement s'arrondit au dirham supérieur. */
    public static BigDecimal versement(BigDecimal total) {
        return total.setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.UNNECESSARY);
    }
}
