package com.gestion.persistent.enums;

/**
 * Incoterms 2020. Les drapeaux indiquent ce qui est déjà inclus dans le prix fournisseur
 * (donc à NE PAS ajouter comme charge d'approche côté acheteur).
 */
public enum Incoterm {
    //            libellé                              origine fret   assur. douane
    EXW("Ex Works (Départ usine)",                    false, false, false, false),
    FCA("Free Carrier (Franco transporteur)",         true,  false, false, false),
    FOB("Free On Board (Franco à bord)",              true,  false, false, false),
    CFR("Cost and Freight (Coût et fret)",            true,  true,  false, false),
    CPT("Carriage Paid To (Port payé jusqu'à)",       true,  true,  false, false),
    CIF("Cost, Insurance and Freight",                true,  true,  true,  false),
    CIP("Carriage and Insurance Paid To",             true,  true,  true,  false),
    DAP("Delivered At Place (Rendu au lieu)",         true,  true,  true,  false),
    DPU("Delivered at Place Unloaded",                true,  true,  true,  false),
    DDP("Delivered Duty Paid (Rendu droits acquittés)", true, true,  true,  true);

    private final String libelle;
    private final boolean inclutTransportOrigine;
    private final boolean inclutFret;
    private final boolean inclutAssurance;
    private final boolean inclutDouaneImport;

    Incoterm(String libelle, boolean inclutTransportOrigine, boolean inclutFret,
             boolean inclutAssurance, boolean inclutDouaneImport) {
        this.libelle = libelle;
        this.inclutTransportOrigine = inclutTransportOrigine;
        this.inclutFret = inclutFret;
        this.inclutAssurance = inclutAssurance;
        this.inclutDouaneImport = inclutDouaneImport;
    }

    public String getLibelle() { return libelle; }
    public boolean isInclutTransportOrigine() { return inclutTransportOrigine; }
    public boolean isInclutFret() { return inclutFret; }
    public boolean isInclutAssurance() { return inclutAssurance; }
    public boolean isInclutDouaneImport() { return inclutDouaneImport; }
}
