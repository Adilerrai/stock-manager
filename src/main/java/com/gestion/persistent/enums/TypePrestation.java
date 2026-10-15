package com.gestion.persistent.enums;

public enum TypePrestation {
    TRANSPORT("Transport, Livraison & Logistique"),
    MAINTENANCE_REPARATION("Maintenance, Réparation & Dépannage"),
    NETTOYAGE_HYGIENE("Nettoyage & Hygiène"),
    GARDIENNAGE_SECURITE("Gardiennage, Sécurité & Surveillance"),
    CONSULTING_JURIDIQUE_COMPTABLE("Consulting, Juridique & Expertise comptable"),
    INFORMATIQUE_TELECOM("Services Informatiques, Cloud & Télécom"),
    TRAVAUX_BATIMENT("Travaux, Aménagement & Bâtiment"),
    MARKETING_COMMUNICATION("Marketing, Design & Communication"),
    SOUS_TRAITANCE("Sous-traitance de services"),
    AUTRE("Autres prestations de services");

    private final String libelle;

    TypePrestation(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
