package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal d'activité du tableau de bord : fil chronologique des opérations
 * (devis créé, BC client, BL livré, facture, encaissement, règlement...).
 *
 * @param content       événements de la page demandée, du plus récent au plus ancien
 * @param totalElements nombre d'événements correspondant aux filtres (types + recherche)
 * @param resume        compteurs par type sur la période (tous types confondus, recherche appliquée)
 */
public record JournalActiviteDTO(List<Evenement> content, long totalElements, int page, int size,
                                 List<Resume> resume) {

    /**
     * @param type        valeur de {@link com.gestion.persistent.enums.TypeEvenementJournal}
     * @param documentId  id du document concerné (devis, BL, facture, paiement...)
     * @param tiers       client ou fournisseur
     * @param statut      statut actuel du document (peut être null)
     * @param utilisateur auteur de l'action quand il est connu
     * @param detail      complément (mode de paiement, motif, désignation...)
     * @param reference   document lié (commande d'origine, facture réglée...)
     */
    public record Evenement(String type, LocalDateTime dateEvenement, Long documentId, String numero,
                            String tiers, BigDecimal montant, String statut, String utilisateur,
                            String detail, String reference) {
    }

    public record Resume(String type, long nombre, BigDecimal montant) {
    }
}
