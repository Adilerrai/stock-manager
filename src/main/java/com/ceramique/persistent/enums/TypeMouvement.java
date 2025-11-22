package com.ceramique.persistent.enums;

public enum TypeMouvement {
    ENTREE_LIVRAISON("Entrée par livraison"),
    SORTIE_VENTE("Sortie par vente"),
    SORTIE_COMMANDE("Sortie par commande"),
    AJUSTEMENT_POSITIF("Ajustement positif"),
    AJUSTEMENT_NEGATIF("Ajustement négatif"),
    TRANSFERT_ENTREE("Transfert entrant"),
    TRANSFERT_SORTIE("Transfert sortant"),
    INVENTAIRE("Correction d'inventaire");
    
    private final String description;
    
    TypeMouvement(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}