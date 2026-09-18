package com.gestion.persistent.enums;

public enum StatutImmobilisation {
    EN_SERVICE("En service"),
    TOTALEMENT_AMORTI("Totalement amorti"),
    CEDE("Cédé (Vendu)"),
    MIS_AU_REBUT("Mis au rebut / Retiré");

    private final String libelle;

    StatutImmobilisation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
