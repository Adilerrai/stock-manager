package com.gestion.persistent.enums;

public enum StatutLivraison {
    BROUILLON("Brouillon"),
    EN_ATTENTE("En attente"),
    EXPEDIE("Expédiée"),
    CACHETE("Cacheté"),
    EN_LIVRAISON("En livraison"),
    PARTIELLE("Partiellement livrée"),
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
