package com.gestion.persistent.enums;

public enum TypeEffet {
    CHEQUE("Chèque"),
    TRAITE("Traite (Lettre de change)"),
    BILLET_A_ORDRE("Billet à ordre");

    private final String libelle;

    TypeEffet(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
