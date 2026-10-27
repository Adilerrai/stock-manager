package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class FusionDemandePrixDTO {
    /**
     * ID de la commande fournisseur au statut BROUILLON existante.
     * Si null, une nouvelle commande sera créée pour le fournisseur.
     */
    private Long commandeId;

    /**
     * ID du fournisseur (obligatoire si commandeId est null).
     */
    private Long fournisseurId;

    /**
     * Liste des IDs des demandes de prix à fusionner.
     * L'offre retenue pour le fournisseur sera automatiquement sélectionnée si non spécifiée dans 'selections'.
     */
    private List<Long> ddpIds = new ArrayList<>();

    /**
     * Liste optionnelle de sélections explicites {ddpId, offreId}.
     */
    private List<DemandePrixOffreItemDTO> selections = new ArrayList<>();

    public FusionDemandePrixDTO() {
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

    public List<Long> getDdpIds() {
        return ddpIds;
    }

    public void setDdpIds(List<Long> ddpIds) {
        this.ddpIds = ddpIds;
    }

    public List<DemandePrixOffreItemDTO> getSelections() {
        return selections;
    }

    public void setSelections(List<DemandePrixOffreItemDTO> selections) {
        this.selections = selections;
    }
}
