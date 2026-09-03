package com.gestion.persistent.enums;

public enum StatutPreparation {
    A_PREPARER("À préparer"),
    EN_COURS("En cours de préparation"),
    PREPARE("Préparé / Prêt à l'expédition"),
    EXPEDIE("Expédié"),
    ANNULE("Annulé");

    private final String libelle;

    StatutPreparation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
