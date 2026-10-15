package com.gestion.persistent.enums;

public enum UniteConsommable {
    PIECE("Pièce / Unité"),
    LITRE("Litre"),
    PAQUET("Paquet"),
    BOITE("Boîte"),
    ROULEAU("Rouleau"),
    RAME("Rame"),
    KG("Kilogramme"),
    METRE("Mètre"),
    LOT("Lot"),
    BIDON("Bidon"),
    CARTOUCHE("Cartouche");

    private final String libelle;

    UniteConsommable(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
