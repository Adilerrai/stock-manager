package com.gestion.persistent.enums;

public enum SensCompte {
    DEBIT("Débit"),
    CREDIT("Crédit");

    private final String libelle;

    SensCompte(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
