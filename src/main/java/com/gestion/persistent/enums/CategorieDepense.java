package com.gestion.persistent.enums;

public enum CategorieDepense {
    SALAIRES("Salaires & Rémunérations"),
    LOYER("Loyer & Charges locatives"),
    ELECTRICITE_EAU("Électricité, Gaz & Eau"),
    TRANSPORT_CARBURANT("Transport, Carburant & Véhicules"),
    COMMUNICATION_INTERNET("Téléphone, Internet & Logiciels"),
    FOURNITURES_BUREAU("Fournitures de bureau & Consommables"),
    ENTRETIEN_MAINTENANCE("Entretien, Réparations & Maintenance"),
    FRAIS_BANCAIRES("Frais & Commissions bancaires"),
    IMPOTS_TAXES("Impôts, Taxes & Cotisations"),
    MARKETING_PUBLICITE("Publicité, Marketing & Événements"),
    AUTRES_CHARGES("Autres charges d'exploitation");

    private final String libelle;

    CategorieDepense(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
