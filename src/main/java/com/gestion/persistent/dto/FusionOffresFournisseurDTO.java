package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class FusionOffresFournisseurDTO {
    /**
     * ID de la commande fournisseur existante au statut BROUILLON.
     * Si null, une nouvelle commande fournisseur sera créée pour le fournisseur.
     */
    private Long commandeId;

    /**
     * ID du fournisseur (optionnel si commandeId est renseigné).
     */
    private Long fournisseurId;

    /**
     * Liste des IDs des offres fournisseurs (OffreFournisseur) à intégrer dans la commande.
     */
    private List<Long> offreIds = new ArrayList<>();

    public FusionOffresFournisseurDTO() {
    }

    public FusionOffresFournisseurDTO(Long commandeId, List<Long> offreIds) {
        this.commandeId = commandeId;
        this.offreIds = offreIds;
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public void setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
    }

    public List<Long> getOffreIds() {
        return offreIds;
    }

    public void setOffreIds(List<Long> offreIds) {
        this.offreIds = offreIds;
    }
}
