package com.gestion.persistent.enums;

/**
 * Types de charges d'approche.
 * - section : ACHEMINEMENT (entre dans la valeur CIF / base douane), DOUANE, LOCAL
 * - incorporableDefaut : entre dans le coût de revient par défaut
 * - nationalAutorise : utilisable sur une commande nationale
 */
public enum TypeCharge {
    TRANSPORT_ORIGINE("Pré-acheminement (usine → port)", "ACHEMINEMENT", true,  CleRepartition.VALEUR,   false),
    FRET_INTERNATIONAL("Facture transporteur (fret)",    "ACHEMINEMENT", true,  CleRepartition.VALEUR,   false),
    ASSURANCE("Assurance transport",                     "ACHEMINEMENT", true,  CleRepartition.VALEUR,   false),
    DROITS_DOUANE("Droits de douane (hors TVA)",         "DOUANE",       true,  CleRepartition.VALEUR,   false),
    TVA_IMPORT("TVA à l'import (récupérable)",           "DOUANE",       false, CleRepartition.VALEUR,   false),
    TRANSITAIRE("Facture transitaire",                   "DOUANE",       true,  CleRepartition.VALEUR,   false),
    MAGASINAGE("Facture magasinage",                     "LOCAL",        true,  CleRepartition.VALEUR,   true),
    PENALITE("Pénalités / surestaries",                  "LOCAL",        true,  CleRepartition.VALEUR,   true),
    TRANSPORT_LOCAL("Transport local",                   "LOCAL",        true,  CleRepartition.VALEUR,   true),
    MANUTENTION("Manutention",                           "LOCAL",        true,  CleRepartition.QUANTITE, true),
    MAIN_OEUVRE("Main d'œuvre",                          "LOCAL",        true,  CleRepartition.QUANTITE, true),
    FRAIS_BANCAIRES("Frais bancaires (Credoc, transfert)", "LOCAL",      true,  CleRepartition.VALEUR,   false),
    AUTRE("Autres charges",                              "LOCAL",        true,  CleRepartition.VALEUR,   true);

    private final String libelle;
    private final String section;
    private final boolean incorporableDefaut;
    private final CleRepartition cleDefaut;
    private final boolean nationalAutorise;

    TypeCharge(String libelle, String section, boolean incorporableDefaut,
               CleRepartition cleDefaut, boolean nationalAutorise) {
        this.libelle = libelle;
        this.section = section;
        this.incorporableDefaut = incorporableDefaut;
        this.cleDefaut = cleDefaut;
        this.nationalAutorise = nationalAutorise;
    }

    public String getLibelle() { return libelle; }
    public String getSection() { return section; }
    public boolean isIncorporableDefaut() { return incorporableDefaut; }
    public CleRepartition getCleDefaut() { return cleDefaut; }
    public boolean isNationalAutorise() { return nationalAutorise; }
}
