package com.gestion.service.compta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Retenue à la source sur la TVA : le calcul seul, sur le montant de ce règlement, jamais sur la facture entière (RAS-01 à RAS-05).
 * Le client est une société privée : le cas 117-V-a, État et organismes publics, n'est pas servi.
 */
public final class RasCalcul {

    public static final Set<String> NATURES = Set.of("MARCHANDISE", "BIEN_EQUIPEMENT", "TRAVAUX", "SERVICE", "SERVICE_EXCLU");
    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal Z = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private RasCalcul() {
    }

    /** Lus par date dans les paramètres fiscaux, avec leur source. */
    public record Bareme(BigDecimal tauxAvecAttestation, BigDecimal tauxSansAttestation, BigDecimal petiteOperation, BigDecimal cumulDuMois) {
    }

    /**
     * {@code nature} ou {@code personnePhysique} nuls : rien n'est supposé, le règlement reste à qualifier.
     * {@code cumulServices} compte tous les services du fournisseur dans le mois, celui-ci compris. {@code seuilCaAtteint} : art. 117-V-c et 247-XXXXVI.
     */
    public record Entree(String nature, Boolean personnePhysique, boolean attestation, boolean seuilCaAtteint, BigDecimal ttc, BigDecimal tauxTva, BigDecimal cumulServices, Bareme bareme) {
    }

    public record Resultat(boolean aQualifier, BigDecimal ht, BigDecimal tva, BigDecimal tauxRetenue, BigDecimal retenue, String article, String raison) {

        public boolean aRetenir() {
            return retenue.signum() > 0;
        }
    }

    public static Resultat calculer(Entree e) {
        BigDecimal tva = e.ttc().multiply(e.tauxTva()).divide(CENT.add(e.tauxTva()), 2, RoundingMode.HALF_UP);
        BigDecimal ht = e.ttc().subtract(tva);
        if (e.nature() == null) {
            return new Resultat(true, ht, tva, Z, Z, null, "Nature de l'achat à préciser.");
        }
        return switch (e.nature()) {
            case "MARCHANDISE" -> sans(ht, tva, "Marchandises destinées à la vente : hors retenue (art. 117-IV et V).");
            case "SERVICE_EXCLU" -> sans(ht, tva, "Service exclu par l'art. 117 : eau, électricité, télécommunications, courtage d'assurance.");
            case "BIEN_EQUIPEMENT", "TRAVAUX" -> e.attestation()
                    ? sans(ht, tva, "Attestation de moins de six mois : pas de retenue.")
                    : retenue(ht, tva, e.bareme().tauxSansAttestation(), "117-IV", "Sans attestation de moins de six mois.");
            case "SERVICE" -> service(e, ht, tva);
            default -> throw new IllegalArgumentException("Nature inconnue : " + e.nature());
        };
    }

    private static Resultat service(Entree e, BigDecimal ht, BigDecimal tva) {
        if (e.personnePhysique() == null) {
            return new Resultat(true, ht, tva, Z, Z, null, "Le fournisseur est-il une personne physique ou morale ? À préciser sur sa fiche.");
        }
        if (e.ttc().compareTo(e.bareme().petiteOperation()) <= 0 && e.cumulServices().compareTo(e.bareme().cumulDuMois()) <= 0) {
            return sans(ht, tva, "Prestation de " + e.bareme().petiteOperation().toPlainString() + " DH TTC au plus, cumul du mois sous " + e.bareme().cumulDuMois().toPlainString() + " DH : hors retenue.");
        }
        if (!e.personnePhysique() && !e.seuilCaAtteint()) {
            return sans(ht, tva, "Entre personnes morales, sous le seuil de chiffre d'affaires en vigueur : pas de retenue (art. 247-XXXXVI).");
        }
        String article = e.personnePhysique() ? "117-V-b" : "117-V-c";
        return e.attestation()
                ? retenue(ht, tva, e.bareme().tauxAvecAttestation(), article, "Service, attestation présentée.")
                : retenue(ht, tva, e.bareme().tauxSansAttestation(), article, "Service, sans attestation de moins de six mois.");
    }

    private static Resultat sans(BigDecimal ht, BigDecimal tva, String raison) {
        return new Resultat(false, ht, tva, Z, Z, null, raison);
    }

    private static Resultat retenue(BigDecimal ht, BigDecimal tva, BigDecimal taux, String article, String raison) {
        return new Resultat(false, ht, tva, taux, tva.multiply(taux).divide(CENT, 2, RoundingMode.HALF_UP), article, raison);
    }

    /** Art. 167 : le versement s'arrondit au dirham supérieur. */
    public static BigDecimal versement(BigDecimal total) {
        return total.setScale(0, RoundingMode.CEILING).setScale(2, RoundingMode.UNNECESSARY);
    }
}
