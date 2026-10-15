package com.gestion.persistent.enums;

public enum CategorieConsommable {
    FOURNITURE_BUREAU("Fournitures de bureau (Papeterie, Stylos, Classeurs)"),
    CARBURANT("Carburant & Lubrifiants (Gasoil, Essence, AdBlue)"),
    HYGIENE_ENTRETIEN("Produits d'hygiène & Entretien"),
    PETIT_EQUIPEMENT("Petit équipement & Outillage consommable"),
    INFORMATIQUE("Consommables informatiques (Cartouches, Toners, Câbles)"),
    PIECE_CONSOMMABLE("Pièces d'usure & Quincaillerie"),
    ALIMENTATION_RECEPTION("Alimentation, Café & Réception"),
    AUTRE("Autres consommables");

    private final String libelle;

    CategorieConsommable(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
