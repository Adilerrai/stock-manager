package com.gestion.persistent.enums;

public enum TypeProduit {
    MARCHANDISE("Marchandise / Produit matériel"),
    SERVICE("Service / Prestation / Main d'œuvre");

    private final String libelle;

    TypeProduit(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean isService() {
        return this == SERVICE;
    }
}
