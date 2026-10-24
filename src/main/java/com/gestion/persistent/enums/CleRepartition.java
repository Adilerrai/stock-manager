package com.gestion.persistent.enums;

public enum CleRepartition {
    VALEUR("Au prorata de la valeur"),
    QUANTITE("Au prorata de la quantité"),
    POIDS("Au prorata du poids"),
    VOLUME("Au prorata du volume"),
    DIRECTE("Directe sur une ligne");

    private final String libelle;

    CleRepartition(String libelle) { this.libelle = libelle; }

    public String getLibelle() { return libelle; }
}
