package com.gestion.persistent.enums;

public enum TypeAmortissement {
    LINEAIRE("Amortissement linéaire constant (prorata temporis)"),
    DEGRESSIF_MAROCAIN("Amortissement dégressif fiscal marocain (Art. 10 CGI)"),
    NON_AMORTISSABLE("Non amortissable (ex: Terrains, Titres)");

    private final String description;

    TypeAmortissement(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
