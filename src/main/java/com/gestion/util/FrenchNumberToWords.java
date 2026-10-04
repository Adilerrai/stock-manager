package com.gestion.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FrenchNumberToWords {

    private static final String[] UNITES = {
        "", "UN", "DEUX", "TROIS", "QUATRE", "CINQ", "SIX", "SEPT", "HUIT", "NEUF",
        "DIX", "ONZE", "DOUZE", "TREIZE", "QUATORZE", "QUINZE", "SEIZE", "DIX-SEPT", "DIX-HUIT", "DIX-NEUF"
    };

    private static final String[] DIZAINES = {
        "", "DIX", "VINGT", "TRENTE", "QUARANTE", "CINQUANTE", "SOIXANTE", "SOIXANTE-DIX", "QUATRE-VINGT", "QUATRE-VINGT-DIX"
    };

    public static String convertir(BigDecimal montant, String devise) {
        if (montant == null) {
            montant = BigDecimal.ZERO;
        }

        montant = montant.setScale(2, RoundingMode.HALF_UP);
        long partieEntiere = Math.abs(montant.longValue());
        int centimes = montant.remainder(BigDecimal.ONE).multiply(new BigDecimal(100)).intValue();
        centimes = Math.abs(centimes);

        String deviseNom = "DIRHAMS";
        String deviseSingulier = "DIRHAM";
        String centimeNom = "CENTIMES";
        String centimeSingulier = "CENTIME";

        if (devise != null && !devise.trim().isEmpty()) {
            String devUpper = devise.trim().toUpperCase();
            if (devUpper.equals("EUR") || devUpper.contains("EURO")) {
                deviseNom = "EUROS";
                deviseSingulier = "EURO";
            } else if (devUpper.equals("USD") || devUpper.contains("DOLLAR")) {
                deviseNom = "DOLLARS";
                deviseSingulier = "DOLLAR";
                centimeNom = "CENTS";
                centimeSingulier = "CENT";
            } else if (!devUpper.equals("MAD")) {
                deviseNom = devUpper;
                deviseSingulier = devUpper;
            }
        }

        StringBuilder sb = new StringBuilder();

        if (montant.compareTo(BigDecimal.ZERO) < 0) {
            sb.append("MOINS ");
        }

        if (partieEntiere == 0) {
            sb.append("ZÉRO ").append(deviseNom);
        } else {
            String texteEntier = convertirNombre(partieEntiere);
            sb.append(texteEntier).append(" ");
            if (partieEntiere == 1) {
                sb.append(deviseSingulier);
            } else {
                sb.append(deviseNom);
            }
        }

        if (centimes > 0) {
            sb.append(" ET ").append(convertirNombre(centimes)).append(" ");
            if (centimes == 1) {
                sb.append(centimeSingulier);
            } else {
                sb.append(centimeNom);
            }
        }

        return sb.toString().trim().replaceAll("\\s+", " ").toUpperCase();
    }

    public static String convertirNombre(long n) {
        if (n == 0) return "ZÉRO";
        if (n < 0) return "MOINS " + convertirNombre(-n);

        if (n >= 1_000_000_000) {
            long milliards = n / 1_000_000_000;
            long reste = n % 1_000_000_000;
            String label = (milliards > 1) ? "MILLIARDS" : "MILLIARD";
            String res = convertirNombre(milliards) + " " + label;
            if (reste > 0) res += " " + convertirNombre(reste);
            return res;
        }

        if (n >= 1_000_000) {
            long millions = n / 1_000_000;
            long reste = n % 1_000_000;
            String label = (millions > 1) ? "MILLIONS" : "MILLION";
            String res = convertirNombre(millions) + " " + label;
            if (reste > 0) res += " " + convertirNombre(reste);
            return res;
        }

        if (n >= 1000) {
            long milliers = n / 1000;
            long reste = n % 1000;
            String res = (milliers == 1) ? "MILLE" : (convertirNombre(milliers) + " MILLE");
            if (reste > 0) res += " " + convertirNombre(reste);
            return res;
        }

        if (n >= 100) {
            long centaines = n / 100;
            long reste = n % 100;
            String res = (centaines == 1) ? "CENT" : (UNITES[(int) centaines] + " CENT");
            if (reste == 0 && centaines > 1) {
                res += "S";
            }
            if (reste > 0) {
                res += " " + convertirNombre(reste);
            }
            return res;
        }

        if (n >= 20) {
            int dizaine = (int) (n / 10);
            int unite = (int) (n % 10);

            if (dizaine == 7) {
                if (unite == 1) return "SOIXANTE ET ONZE";
                return "SOIXANTE-" + UNITES[10 + unite];
            }
            if (dizaine == 8) {
                if (unite == 0) return "QUATRE-VINGTS";
                return "QUATRE-VINGT-" + UNITES[unite];
            }
            if (dizaine == 9) {
                return "QUATRE-VINGT-" + UNITES[10 + unite];
            }

            if (unite == 1) {
                return DIZAINES[dizaine] + " ET UN";
            }
            if (unite > 1) {
                return DIZAINES[dizaine] + "-" + UNITES[unite];
            }
            return DIZAINES[dizaine];
        }

        return UNITES[(int) n];
    }
}
