package com.gestion.service.compta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Tout ce qu'une règle a besoin de savoir, en dehors de la pièce.
 *
 * @param correspondances compte par « TYPE|CLE »
 * @param remiseMode      NET_TVA_FACTUREE ou NET_TVA_REDUITE
 * @param ventilation     ce que l'utilisateur a précisé et que la source ne porte pas
 * @param evenementsPasses événements déjà passés sur cette pièce : un effet tire sa position de là, son statut n'étant pas fiable
 * @param prealablePasse  la pièce préalable est au journal : le paiement d'un effet
 */
public record ContexteRegles(Map<String, String> correspondances, String remiseMode, List<LigneVentilation> ventilation, Complement complement,
                             Set<String> evenementsPasses, boolean prealablePasse) {

    public record Complement(Long compteFinancierId, LocalDate dateEffet, String referenceDocument) {
        public static final Complement AUCUN = new Complement(null, null, null);
    }

    public record LigneVentilation(String compte, BigDecimal ht, BigDecimal tauxTva, BigDecimal tva, boolean deductible, boolean immobilisation,
                                   String numeroFactureFournisseur) {
    }

    public Optional<String> compte(String typeCle, String cle) {
        return Optional.ofNullable(correspondances.get(typeCle + "|" + cle));
    }
}
