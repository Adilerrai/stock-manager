package com.gestion.persistent.enums;

public enum TypeEffet {
    CHEQUE("Chèque"),
    TRAITE("Traite (Lettre de change)"),
    LCN("LCN (Lettre de change normalisée)"),
    BILLET_A_ORDRE("Billet à ordre");

    private final String libelle;

    TypeEffet(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
