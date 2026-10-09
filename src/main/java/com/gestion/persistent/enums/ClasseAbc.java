package com.gestion.persistent.enums;

public enum ClasseAbc {
    A("Classe A (80% de la valeur)"),
    B("Classe B (15% de la valeur)"),
    C("Classe C (5% de la valeur)");

    private final String description;

    ClasseAbc(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
