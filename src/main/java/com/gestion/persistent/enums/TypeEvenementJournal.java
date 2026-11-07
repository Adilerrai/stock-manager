package com.gestion.persistent.enums;

/**
 * Types d'événements affichés dans le journal d'activité du tableau de bord.
 * Chaque type correspond à une date réelle portée par un document (création, expédition, livraison, paiement...).
 */
public enum TypeEvenementJournal {
    // Ventes
    DEVIS_CREE,
    BC_CLIENT_CREE,
    BL_CREE,
    BL_EXPEDIE,
    BL_LIVRE,
    FACTURE_CREEE,
    FACTURE_ANNULEE,
    AVOIR_CLIENT,
    // Achats
    BC_FOURNISSEUR_CREE,
    RECEPTION_FOURNISSEUR,
    FACTURE_ACHAT_CREEE,
    AVOIR_FOURNISSEUR,
    // Trésorerie
    ENCAISSEMENT,
    REGLEMENT_FOURNISSEUR,
    DEPENSE
}
