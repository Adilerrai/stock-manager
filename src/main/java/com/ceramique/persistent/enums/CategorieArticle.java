package com.ceramique.persistent.enums;

public enum CategorieArticle {
    SOL("Sol"),
    MUR("Mur"),
    EXTERIEUR("Extérieur"),
    FAIENCE("Faïence"),
    GRES("Grès cérame"),
    MARBRE("Marbre"),
    GRANITE("Granite"),
    PORCELAINE("Porcelaine"),
    MOSAIQUE("Mosaïque"),
    ACCESSOIRES("Accessoires");

    private final String libelle;

    CategorieArticle(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

