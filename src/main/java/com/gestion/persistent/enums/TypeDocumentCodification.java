package com.gestion.persistent.enums;

public enum TypeDocumentCodification {
    FACTURE_CLIENT("Facture Client", "FACT", "{PREFIX}-{AAAA}-{NUM}", 3),
    FACTURE_FOURNISSEUR("Facture Fournisseur (Achat)", "FA", "{PREFIX}-{AAAA}-{NUM}", 3),
    COMMANDE_CLIENT("Commande Client", "CC", "{PREFIX}-{AAAA}-{NUM}", 3),
    COMMANDE_FOURNISSEUR("Commande Fournisseur (Achat)", "CF", "{PREFIX}-{AAAA}-{NUM}", 3),
    BL_CLIENT("Bon de Livraison Client", "BL", "{PREFIX}-{AAAA}-{NUM}", 3),
    BL_FOURNISSEUR("Bon de Réception Fournisseur", "BR", "{PREFIX}-{AAAA}-{NUM}", 3),
    DEVIS("Devis Client", "DEV", "{PREFIX}-{AAAA}-{NUM}", 3),
    AVOIR_CLIENT("Avoir Client", "AVR-CLI", "{PREFIX}-{AAAA}-{NUM}", 3),
    AVOIR_FOURNISSEUR("Avoir Fournisseur", "AVR-FRS", "{PREFIX}-{AAAA}-{NUM}", 3);

    private final String libelle;
    private final String prefixeDefaut;
    private final String formatDefaut;
    private final int longueurSequenceDefaut;

    TypeDocumentCodification(String libelle, String prefixeDefaut, String formatDefaut, int longueurSequenceDefaut) {
        this.libelle = libelle;
        this.prefixeDefaut = prefixeDefaut;
        this.formatDefaut = formatDefaut;
        this.longueurSequenceDefaut = longueurSequenceDefaut;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getPrefixeDefaut() {
        return prefixeDefaut;
    }

    public String getFormatDefaut() {
        return formatDefaut;
    }

    public int getLongueurSequenceDefaut() {
        return longueurSequenceDefaut;
    }
}
