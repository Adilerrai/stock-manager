package com.gestion.persistent.enums;

public enum StatutConformiteAchat {
    CONFORME("Conforme aux commandes et réceptions"),
    LIVRAISON_PARTIELLE("Livraison partielle / Quantités manquantes en stock"),
    SURFACTURATION_QUANTITE("Surfacturation : Facturé supérieur à la quantité livrée"),
    PRIX_SUPERIEUR_COMMANDE("Prix unitaire facturé supérieur au prix commandé"),
    LITIGE_MAJEUR("Litige majeur : Écarts cumulés de prix et de quantité");

    private final String libelle;

    StatutConformiteAchat(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
