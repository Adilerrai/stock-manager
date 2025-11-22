package com.ceramique.persistent.enums;

public enum StatutLivraison {
    EN_ATTENTE("En attente"),
    EN_LIVRAISON("En livraison"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutLivraison(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}