package com.gestion.persistent.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatutCommandeClient {
    BROUILLON,
    CONFIRMEE,
    EN_PREPARATION,
    PRETE,
    LIVREE_PARTIELLE,
    LIVREE,
    BACKORDER,
    RELIQUAT,
    FACTUREE,
    ANNULEE;

    @JsonCreator
    public static StatutCommandeClient fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if ("LIVREE_PARTIELLEMENT".equals(v) || "PARTIELLEMENT_LIVREE".equals(v) || "PARTIELLE".equals(v)) {
            return LIVREE_PARTIELLE;
        }
        for (StatutCommandeClient s : values()) {
            if (s.name().equalsIgnoreCase(v)) {
                return s;
            }
        }
        return null;
    }
}
