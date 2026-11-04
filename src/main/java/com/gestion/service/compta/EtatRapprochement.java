package com.gestion.service.compta;

import java.math.BigDecimal;

/**
 * L'état de rapprochement, sans base. Les sens se croisent : un débit de 5141 correspond à un crédit du relevé.
 *
 * @param soldeBanqueRectifie relevé + écritures au débit non pointées - écritures au crédit non pointées
 * @param soldeComptableRectifie comptable + crédits du relevé non pointés - débits du relevé non pointés
 */
public record EtatRapprochement(BigDecimal soldeBanqueRectifie, BigDecimal soldeComptableRectifie, BigDecimal ecart) {

    public static EtatRapprochement calculer(BigDecimal soldeReleve, BigDecimal ecrituresDebit, BigDecimal ecrituresCredit, BigDecimal soldeComptable, BigDecimal creditsReleve, BigDecimal debitsReleve) {
        BigDecimal banque = soldeReleve.add(ecrituresDebit).subtract(ecrituresCredit);
        BigDecimal comptable = soldeComptable.add(creditsReleve).subtract(debitsReleve);
        return new EtatRapprochement(banque, comptable, banque.subtract(comptable));
    }
}
