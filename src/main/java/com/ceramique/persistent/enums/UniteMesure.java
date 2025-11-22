package com.ceramique.persistent.enums;

public enum UniteMesure {
    M2("Mètre carré (m²)"),
    PIECE("Pièce"),
    KG("Kilogramme"),
    LITRE("Litre"),
    METRE("Mètre");

    private final String label;

    UniteMesure(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}