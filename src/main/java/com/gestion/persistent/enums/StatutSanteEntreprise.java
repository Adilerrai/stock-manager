package com.gestion.persistent.enums;

public enum StatutSanteEntreprise {
    EXCELLENTE("Excellente santé financière & opérationnelle", "#10b981"),
    BONNE("Bonne santé avec rentabilité solide", "#3b82f6"),
    VIGILANCE("Zone de vigilance - Points de risque détectés", "#f59e0b"),
    CRITIQUE("Alerte critique - Actions urgentes requises", "#ef4444");

    private final String libelle;
    private final String couleur;

    StatutSanteEntreprise(String libelle, String couleur) {
        this.libelle = libelle;
        this.couleur = couleur;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCouleur() {
        return couleur;
    }
}
