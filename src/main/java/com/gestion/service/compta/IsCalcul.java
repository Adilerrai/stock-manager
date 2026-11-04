package com.gestion.service.compta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Le calcul de l'IS, sans base. Taux proportionnel sur tout le bénéfice, jamais de tranches progressives ni de somme à déduire (IS-01).
 * Impôt dû : le plus élevé de l'IS et de la cotisation minimale, sans crédit de CM sur l'IS suivant (CGI art. 144-I-E).
 */
public final class IsCalcul {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal CENT = new BigDecimal("100");
    private static final BigDecimal DIX = BigDecimal.TEN;
    private static final BigDecimal UN_MILLION = new BigDecimal("1000000");
    private static final BigDecimal CINQ_MILLIONS = new BigDecimal("5000000");
    private static final BigDecimal DIX_MILLIONS = new BigDecimal("10000000");
    private static final BigDecimal QUARANTE_MILLIONS = new BigDecimal("40000000");
    private static final BigDecimal TROIS_CENT_MILLE = new BigDecimal("300000");
    private static final BigDecimal CENT_MILLIONS = new BigDecimal("100000000");

    /** @param tauxCss zéro hors du champ de la contribution */
    public record Entree(BigDecimal resultatFiscal, BigDecimal tauxIs, BigDecimal baseCm, BigDecimal tauxCm, BigDecimal minimumCm, boolean cmExoneree, BigDecimal acomptes, BigDecimal excedentAnterieur, BigDecimal tauxCss) {
    }

    public record Resultat(BigDecimal baseArrondie, BigDecimal isTheorique, BigDecimal cm, BigDecimal impotDu, BigDecimal reliquat, BigDecimal excedent, BigDecimal css) {
    }

    private IsCalcul() {
    }

    public static Resultat calculer(Entree e) {
        BigDecimal base = e.resultatFiscal().signum() > 0 ? dizaineSuperieure(e.resultatFiscal()) : Z;
        BigDecimal is = pourcent(base, e.tauxIs());
        BigDecimal cm = e.cmExoneree() ? Z : pourcent(e.baseCm(), e.tauxCm()).max(e.minimumCm());
        BigDecimal impot = is.max(cm);
        BigDecimal verse = e.acomptes().add(e.excedentAnterieur());
        return new Resultat(base, is, cm, impot, impot.subtract(verse).max(Z), verse.subtract(impot).max(Z), pourcent(base, e.tauxCss()));
    }

    /** Art. 167 : la base s'arrondit à la dizaine supérieure. */
    public static BigDecimal dizaineSuperieure(BigDecimal montant) {
        return montant.divide(DIX, 0, RoundingMode.CEILING).multiply(DIX).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal pourcent(BigDecimal base, BigDecimal taux) {
        return base.multiply(taux).divide(CENT, 2, RoundingMode.HALF_UP);
    }

    /** La clé de taux : la tranche du bénéfice pour la catégorie générale, la catégorie elle-même sinon. */
    public static String cleDeTaux(String categorie, BigDecimal benefice) {
        if (categorie != null && !"GENERAL".equals(categorie)) {
            return categorie;
        }
        if (benefice.compareTo(TROIS_CENT_MILLE) <= 0) {
            return "BN_JUSQUA_300K";
        }
        if (benefice.compareTo(UN_MILLION) < 0) {
            return "BN_300K_A_1M";
        }
        return benefice.compareTo(CENT_MILLIONS) < 0 ? "BN_1M_A_100M" : "BN_100M_ET_PLUS";
    }

    /** Art. 267 à 273 : proportionnelle au bénéfice, à partir d'un million de dirhams. Rien en dessous. */
    public static String cleCss(BigDecimal benefice) {
        if (benefice.compareTo(UN_MILLION) < 0) {
            return null;
        }
        if (benefice.compareTo(CINQ_MILLIONS) < 0) {
            return "T1";
        }
        if (benefice.compareTo(DIX_MILLIONS) < 0) {
            return "T2";
        }
        return benefice.compareTo(QUARANTE_MILLIONS) < 0 ? "T3" : "T4";
    }

    /** IS-05 : 25 % de l'IS théorique, arrondi au dirham supérieur. */
    public static BigDecimal acompte(BigDecimal beneficeReference, BigDecimal tauxIs, BigDecimal part) {
        return pourcent(pourcent(beneficeReference.max(Z), tauxIs), part).setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.HALF_UP);
    }

    /** IS-04 : avant l'expiration des 3e, 6e, 9e et 12e mois suivant l'ouverture, et non au calendrier civil. */
    public static LocalDate echeanceAcompte(LocalDate ouverture, int numero) {
        return ouverture.plusMonths(3L * numero).minusDays(1);
    }

    /** IS-03 : 36 mois depuis le début d'exploitation, dans la limite de 60 mois depuis la constitution. */
    public static boolean exonereDeCm(LocalDate constitution, LocalDate debutExploitation, LocalDate finExercice) {
        if (debutExploitation == null) {
            return false;
        }
        boolean dans36 = !finExercice.isAfter(debutExploitation.plusMonths(36));
        boolean dans60 = constitution == null || !finExercice.isAfter(constitution.plusMonths(60));
        return dans36 && dans60;
    }
}
