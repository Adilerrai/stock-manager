package com.gestion.persistent.enums;

/**
 * Nature fonctionnelle de l'avoir :
 * - RETOUR_MARCHANDISE : retour physique de marchandises avec impact stock (si remettreEnStock=true) et financier
 * - AVOIR_COMMERCIAL : remise / geste commercial / déduction financière sans aucun mouvement de stock
 * - ERREUR_FACTURATION : correction d'un écart de facturation sans retour de marchandise
 */
public enum NatureAvoir {
    RETOUR_MARCHANDISE("Retour de marchandise"),
    AVOIR_COMMERCIAL("Avoir commercial / Remise financière"),
    ERREUR_FACTURATION("Correction d'erreur de facturation");

    private final String libelle;

    NatureAvoir(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
