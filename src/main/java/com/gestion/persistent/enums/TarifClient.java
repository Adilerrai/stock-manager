package com.gestion.persistent.enums;

public enum TarifClient {
    DETAIL("Tarif Détail / Particulier"),
    REVENDEUR("Tarif Revendeur / Professionnel"),
    GROSSISTE("Tarif Grossiste / Grand Compte");

    private final String libelle;

    TarifClient(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
