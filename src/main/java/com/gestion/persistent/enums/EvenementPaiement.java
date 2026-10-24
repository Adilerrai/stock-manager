package com.gestion.persistent.enums;

public enum EvenementPaiement {
    COMMANDE("À la commande"),
    AVANT_EXPEDITION("Avant expédition"),
    EXPEDITION("À l'expédition"),
    CONTRE_DOCUMENTS("Contre documents (B/L)"),
    ARRIVEE("À l'arrivée"),
    RECEPTION("À la réception"),
    FACTURE("À la facture");

    private final String libelle;

    EvenementPaiement(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
