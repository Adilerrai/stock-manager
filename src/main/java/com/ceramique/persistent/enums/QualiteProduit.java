package com.ceramique.persistent.enums;

public enum QualiteProduit {
    PREMIERE_QUALITE("Première qualité"),
    DEUXIEME_QUALITE("Deuxième qualité"),
    TROISIEME_QUALITE("Troisième qualité");

    private final String libelle;

    QualiteProduit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}