package com.ceramique.persistent.enums;

public enum CategorieClient {
    PARTICULIER("Particulier"),
    PROFESSIONNEL("Professionnel"),
    CHANTIER("Chantier"),
    ARCHITECTE("Architecte"),
    ENTREPRISE("Entreprise");

    private final String libelle;

    CategorieClient(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

