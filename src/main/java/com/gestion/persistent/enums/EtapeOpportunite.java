package com.gestion.persistent.enums;

public enum EtapeOpportunite {
    NOUVEAU(1),
    QUALIFIE(2),
    PROPOSITION(3),
    NEGOCIATION(4),
    GAGNE(5),
    PERDU(5);

    private final int ordre;

    EtapeOpportunite(int ordre) {
        this.ordre = ordre;
    }

    public int getOrdre() {
        return ordre;
    }

    public boolean isTermine() {
        return this == GAGNE || this == PERDU;
    }

    public boolean canTransitionTo(EtapeOpportunite target) {
        if (this == target) {
            return true;
        }
        if (this == GAGNE || this == PERDU) {
            return false; // état terminal
        }
        if (target == PERDU) {
            return true; // peut être perdu à tout moment avec motif
        }
        // Autorise le passage à l'étape suivante immédiate ou gagné depuis proposition/négociation
        return target.ordre >= this.ordre;
    }
}
