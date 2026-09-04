package com.gestion.persistent.enums;

public enum TypeJournal {
    VENTES("Ventes"),
    ACHATS("Achats"),
    BANQUE("Banque"),
    CAISSE("Caisse"),
    OPERATIONS_DIVERSES("Opérations Diverses");

    private final String libelle;

    TypeJournal(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
