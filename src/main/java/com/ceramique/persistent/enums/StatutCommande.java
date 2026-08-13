package com.ceramique.persistent.enums;

public enum StatutCommande {
    BROUILLON("Brouillon"),
    PASSEE("Passée"),
    PARTIELLE("Partiellement livrée"),
    LIVREE("Livrée"),
    VALIDEE("Validée"),
    ANNULEE("Annulée");

    private final String label;

    StatutCommande(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}