package com.gestion.persistent.enums;

public enum TypeMouvementTresorerie {
    RETRAIT_ESPECES_GERANT("Retrait d'espèces par la direction / gérant", -1),
    DEPOT_BANQUE("Versement d'espèces vers la banque", 0), // débit caisse, crédit banque
    TRANSFERT_INTERNE("Transfert interne de fonds", 0),
    APPORT_FONDS("Apport de fonds / Alimentation de caisse", 1),
    ENCAISSEMENT_DIVERS("Encaissement divers", 1),
    DECAISSEMENT_DIVERS("Décaissement divers", -1),
    AJUSTEMENT_SOLDE("Ajustement / Correction de solde", 0);

    private final String libelle;
    private final int sens; // 1 = Entrée, -1 = Sortie, 0 = Transfert / Neutre

    TypeMouvementTresorerie(String libelle, int sens) {
        this.libelle = libelle;
        this.sens = sens;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getSens() {
        return sens;
    }
}
