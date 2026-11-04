package com.gestion.service.compta;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un montant dans une phrase destinée au comptable : 14 400,00. Ailleurs, un montant reste une chaîne décimale du contrat. */
final class MontantTexte {

    private MontantTexte() {
    }

    static String fr(BigDecimal montant) {
        String texte = montant.setScale(2, RoundingMode.HALF_UP).toPlainString();
        boolean negatif = texte.startsWith("-");
        String net = negatif ? texte.substring(1) : texte;
        int point = net.indexOf('.');
        StringBuilder entier = new StringBuilder(net.substring(0, point));
        for (int i = entier.length() - 3; i > 0; i -= 3) {
            entier.insert(i, '\u00A0');
        }
        return (negatif ? "-" : "") + entier + "," + net.substring(point + 1);
    }
}
