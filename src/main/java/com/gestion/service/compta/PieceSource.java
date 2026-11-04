package com.gestion.service.compta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Une pièce du commercial, déjà lue. Les règles ne voient que cela : aucune entité, aucun dépôt.
 *
 * @param attributs ce qui est propre à un type de pièce : mode de paiement, nature d'avoir, dates d'un effet...
 */
public record PieceSource(String type, Long id, String numero, LocalDate date, Long pointDeVenteId, TiersSource tiers, BigDecimal montant,
                          BigDecimal ht, BigDecimal tva, BigDecimal ttc, BigDecimal remise, String statut, boolean annulee, LocalDate dateAnnulation,
                          String empreinte, Map<String, String> attributs, List<LigneSource> lignes) {

    /** @param type CLIENT, FOURNISSEUR ou PRESTATAIRE ; id nul : le tiers « clients divers » */
    public record TiersSource(String type, Long id, String nom, String pays) {
    }

    public record LigneSource(Long produitId, String typeProduit, Long categorieId, BigDecimal tauxTva, BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    public String attr(String cle) {
        String valeur = attributs.get(cle);
        return valeur == null || valeur.isEmpty() ? null : valeur;
    }

    public boolean vrai(String cle) {
        return "true".equalsIgnoreCase(attributs.get(cle));
    }

    public LocalDate dateAttr(String cle) {
        String valeur = attr(cle);
        return valeur == null ? null : LocalDate.parse(valeur.substring(0, 10));
    }
}
