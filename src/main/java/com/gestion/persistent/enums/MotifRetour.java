package com.gestion.persistent.enums;

public enum MotifRetour {
    PRODUIT_DEFECTUEUX_CASSE("Produit défectueux ou cassé"),
    ERREUR_PREPARATION_MAGASIN("Erreur de préparation / Erreur magasin"),
    ERREUR_COMMANDE_CLIENT("Erreur de commande par le client"),
    PRODUIT_NON_CONFORME("Produit non conforme (couleur, teinte, dimension)"),
    PRODUIT_PERIME("Produit périmé / date limite dépassée"),
    RETARD_LIVRAISON("Retard excessif de livraison"),
    AUTRE("Autre motif exceptionnel");

    private final String libelle;

    MotifRetour(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
