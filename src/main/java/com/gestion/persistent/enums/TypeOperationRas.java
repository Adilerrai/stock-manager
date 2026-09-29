package com.gestion.persistent.enums;

/**
 * Types d'opérations assujetties à la Retenue à la Source (RAS) selon le Code Général des Impôts marocain (CGI).
 */
public enum TypeOperationRas {
    /**
     * Biens d'équipement et travaux (Art. 117-VI CGI) :
     * 100% de la TVA due si absence d'attestation de régularité fiscale (ARF < 6 mois), 0% si attestation valide.
     */
    BIENS_EQUIPEMENT_TRAVAUX,

    /**
     * Prestations de services visées à l'art. 89-I (Art. 117-VI CGI) :
     * 75% de la TVA retenue si attestation ARF valide, 100% de la TVA si absence d'attestation.
     */
    PRESTATIONS_SERVICES,

    /**
     * Prestataires étrangers non-résidents au Maroc :
     * 100% de la TVA retenue.
     */
    PRESTATAIRES_NON_RESIDENTS,

    /**
     * Retenue sur les loyers commerciaux payés à des personnes physiques (Loi de Finances) : 5%.
     */
    LOYERS_COMMERCIAUX,

    /**
     * Retenue sur honoraires des professions libérales (avocats, experts, notaires, architectes - Art. 157 CGI) : 10% (ou 5%).
     */
    HONORAIRES_LIBERAUX,

    /**
     * Autre retenue personnalisée.
     */
    AUTRE
}
