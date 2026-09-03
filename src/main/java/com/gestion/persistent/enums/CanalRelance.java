package com.gestion.persistent.enums;

public enum CanalRelance {
    TELEPHONE("Appel téléphonique"),
    EMAIL("Email de relance"),
    VISITE_COMMERCIALE("Visite commerciale / Présentiel"),
    COURRIER_MISE_EN_DEMEURE("Courrier officiel / Mise en demeure"),
    WHATSAPP_SMS("Message WhatsApp / SMS");

    private final String libelle;

    CanalRelance(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
