package com.gestion.persistent.enums;

public enum StatutReservationClient {
    BROUILLON("Brouillon"),
    SOUMISE("Soumise"),
    EN_COURS_DEVIS("Devis en préparation"),
    DEVIS_ENVOYE("Devis envoyé au client"),
    ACCEPTEE("Acceptée / En commande"),
    REFUSEE("Refusée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutReservationClient(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
