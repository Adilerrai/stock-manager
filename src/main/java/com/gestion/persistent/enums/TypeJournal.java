package com.gestion.persistent.enums;

public enum TypeJournal {
    VENTES("Ventes"),
    ACHATS("Achats"),
    BANQUE("Banque"),
    CAISSE("Caisse"),
    OPERATIONS_DIVERSES("Opérations Diverses"),
    A_NOUVEAUX("À-Nouveaux (Bilan d'ouverture)"),
    PAIE("Paie & Salaires");

    private final String libelle;

    TypeJournal(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
