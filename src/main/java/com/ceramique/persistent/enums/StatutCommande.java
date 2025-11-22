package com.ceramique.persistent.enums;

public enum StatutCommande {
    EN_ATTENTE("En attente"),
    CONFIRMEE("Confirmée"),
    EN_COURS("En cours"),
    LIVREE("Livrée"),
    ANNULEE("Annulée");

    private final String label;

    StatutCommande(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}