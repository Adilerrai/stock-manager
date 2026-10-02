package com.gestion.persistent.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UniteMesure {
    M2("Mètre carré (m²)"),
    PIECE("Pièce"),
    KG("Kilogramme"),
    LITRE("Litre"),
    METRE("Mètre"),
    SAC("Sac"),
    CARTON("Carton"),
    BOITE("Boîte"),
    ROULEAU("Rouleau"),
    PAQUET("Paquet"),
    PALETTE("Palette"),
    LOT("Lot");

    private final String label;

    UniteMesure(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static UniteMesure fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (UniteMesure u : UniteMesure.values()) {
            if (u.name().equalsIgnoreCase(value.trim()) || u.getLabel().equalsIgnoreCase(value.trim())) {
                return u;
            }
        }
        try {
            return UniteMesure.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PIECE;
        }
    }
}

