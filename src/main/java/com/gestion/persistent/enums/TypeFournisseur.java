package com.gestion.persistent.enums;

public enum TypeFournisseur {
    NATIONAL("National"),
    INTERNATIONAL("International");

    private final String libelle;

    TypeFournisseur(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
