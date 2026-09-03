package com.gestion.persistent.enums;

public enum TypeCompteFinancier {
    CAISSE_PHYSIQUE("Caisse physique (Espèces)"),
    COMPTE_BANCAIRE("Compte bancaire (Chèque, Virement, CB)");

    private final String libelle;

    TypeCompteFinancier(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
