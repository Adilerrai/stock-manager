package com.gestion.persistent.enums;

public enum VitesseRotation {
    RAPIDE("Rotation rapide (> 6 rot/an)"),
    MOYENNE("Rotation normale (2 à 6 rot/an)"),
    LENTE("Rotation lente (< 2 rot/an)");

    private final String description;

    VitesseRotation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
