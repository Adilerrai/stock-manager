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
    ACCESSOIRES("Accessoires"),
    COSMETIQUE("Cosmétique"),
    DROGUERIE("Droguerie"),
    EPI("Équipement de Protection Individuelle"),
    GENERIQUE("Générique"),
    SOIN("Soin & Beauté"),
    PEINTURE("Peinture & Solvants"),
    OUTILLAGE("Outillage"),
    HYGIENE("Hygiène & Entretien");

    private final String libelle;

    CategorieArticle(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}

