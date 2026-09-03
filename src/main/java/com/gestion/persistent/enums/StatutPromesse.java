package com.gestion.persistent.enums;

public enum StatutPromesse {
    EN_ATTENTE("En attente de paiement"),
    HONOREE_TENUE("Honorée / Paiement encaissé"),
    ROMPUE_NON_TENUE("Rompue / Délai dépassé non respecté");

    private final String libelle;

    StatutPromesse(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
