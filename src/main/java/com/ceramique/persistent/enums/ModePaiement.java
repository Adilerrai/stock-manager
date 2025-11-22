package com.ceramique.persistent.enums;

public enum ModePaiement {
    ESPECES("Espèces"),
    CARTE_BANCAIRE("Carte bancaire"),
    CHEQUE("Chèque"),
    VIREMENT("Virement"),
    CREDIT("Crédit");

    private final String libelle;

    ModePaiement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

