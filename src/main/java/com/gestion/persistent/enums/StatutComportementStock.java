package com.gestion.persistent.enums;

public enum StatutComportementStock {
    RUPTURE("Rupture de stock"),
    SOUS_STOCK("Sous-stock (Réapprovisionnement urgent)"),
    SUR_STOCK("Sur-stock (Capital immobilisé)"),
    NORMAL("Rotation saine");

    private final String libelle;

    StatutComportementStock(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
