package com.gestion.service.compta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Le calcul d'une dotation, sans base. Prorata temporis en jours de 30 sur 360 ; la dernière annuité absorbe l'arrondi pour que la valeur nette arrive exactement à 0.
 * Le plan est une prévision : ce qui compte pour les états, c'est ce qui a été passé.
 */
public final class PlanAmortissement {

    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TRENTE = new BigDecimal("30");
    private static final BigDecimal TROIS_CENT_SOIXANTE = new BigDecimal("360");
    private static final BigDecimal DOUZE = new BigDecimal("12");
    private static final int MOIS_DE_REGULARISATION = 60;

    /** @param coefficient dégressif seulement ; service nul : le bien n'est pas encore en service et rien ne se dote */
    public record Fiche(BigDecimal brut, BigDecimal reprise, String mode, int dureeMois, BigDecimal coefficient, LocalDate service) {
    }

    public record Annuite(LocalDate du, LocalDate au, BigDecimal montant) {
    }

    private PlanAmortissement() {
    }

    public static LocalDate finDePlan(Fiche f) {
        return f.service().plusMonths(f.dureeMois()).minusDays(1);
    }

    /** Jours de 30 sur 360, bornes comprises : un mois plein vaut 30 jours, février compris. */
    public static int jours360(LocalDate du, LocalDate au) {
        int j1 = Math.min(du.getDayOfMonth(), 30);
        boolean finFevrier = au.getMonthValue() == 2 && au.getDayOfMonth() == au.lengthOfMonth();
        int j2 = finFevrier ? 30 : Math.min(au.getDayOfMonth(), 30);
        return (au.getYear() - du.getYear()) * 360 + (au.getMonthValue() - du.getMonthValue()) * 30 + (j2 - j1) + 1;
    }

    /**
     * La dotation de la tranche [debut, fin], fin étant la fin de l'exercice ou la date de sortie.
     *
     * @param passees les dotations passées et non contre-passées, sans la reprise
     */
    public static BigDecimal dotation(Fiche f, BigDecimal passees, LocalDate debut, LocalDate fin) {
        if ("NON_AMORTISSABLE".equals(f.mode()) || f.service() == null) {
            return Z;
        }
        BigDecimal reste = f.brut().subtract(f.reprise()).subtract(passees);
        if (reste.signum() <= 0) {
            return Z;
        }
        LocalDate depart = f.service().isAfter(debut) ? f.service() : debut;
        if (depart.isAfter(fin)) {
            return Z;
        }
        LocalDate terme = finDePlan(f);
        if (!terme.isAfter(fin)) {
            return reste;
        }
        BigDecimal jours = BigDecimal.valueOf(jours360(depart, fin));
        BigDecimal montant;
        if ("DEGRESSIF".equals(f.mode())) {
            BigDecimal net = f.brut().subtract(f.reprise()).subtract(passees);
            // Taux dégressif = coefficient / durée en années ; la base est la valeur résiduelle.
            montant = net.multiply(f.coefficient()).multiply(DOUZE).multiply(jours).divide(BigDecimal.valueOf(f.dureeMois()).multiply(TROIS_CENT_SOIXANTE), 2, RoundingMode.HALF_UP);
        } else {
            montant = f.brut().multiply(jours).divide(BigDecimal.valueOf(f.dureeMois()).multiply(TRENTE), 2, RoundingMode.HALF_UP);
        }
        return montant.min(reste);
    }

    /** La prévision à partir d'un exercice, en reconduisant ses dates d'année en année. Elle ne s'écrit nulle part. */
    public static List<Annuite> prevision(Fiche f, BigDecimal passees, LocalDate exerciceDu, LocalDate exerciceAu) {
        List<Annuite> plan = new ArrayList<>();
        BigDecimal cumul = passees;
        LocalDate du = exerciceDu;
        LocalDate au = exerciceAu;
        for (int i = 0; i < 100; i++) {
            BigDecimal montant = dotation(f, cumul, du, au);
            if (montant.signum() > 0) {
                LocalDate debut = f.service().isAfter(du) ? f.service() : du;
                LocalDate terme = finDePlan(f);
                plan.add(new Annuite(debut, terme.isBefore(au) ? terme : au, montant));
                cumul = cumul.add(montant);
            }
            if (f.brut().subtract(f.reprise()).subtract(cumul).signum() <= 0 || f.service() == null || "NON_AMORTISSABLE".equals(f.mode())) {
                break;
            }
            du = du.plusYears(1);
            au = au.plusYears(1);
        }
        return plan;
    }

    /** Art. 102 : la TVA déduite se reverse pour ce qui reste à courir sur 60 mois. Cas officiel : 200 000 x 45 / 60 = 150 000. */
    public static BigDecimal reversementTva(BigDecimal tvaDeduite, LocalDate acquisition, LocalDate sortie) {
        long ecoules = ChronoUnit.MONTHS.between(acquisition, sortie);
        if (ecoules >= MOIS_DE_REGULARISATION || tvaDeduite.signum() == 0) {
            return Z;
        }
        return tvaDeduite.multiply(BigDecimal.valueOf(MOIS_DE_REGULARISATION - ecoules)).divide(BigDecimal.valueOf(MOIS_DE_REGULARISATION), 2, RoundingMode.HALF_UP);
    }
}
