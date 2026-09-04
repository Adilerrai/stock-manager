package com.gestion.persistent.enums;

public enum TypeNotificationAlerte {
    STOCK_FAIBLE("Stock faible / Rupture"),
    FACTURE_IMPAYEE("Facture client impayée"),
    ECHEANCE_EFFET("Échéance de chèque / effet"),
    COMMANDE_EN_ATTENTE("Commande en attente de traitement");

    private final String libelle;

    TypeNotificationAlerte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
