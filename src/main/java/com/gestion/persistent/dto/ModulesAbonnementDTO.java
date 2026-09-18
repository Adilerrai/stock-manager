package com.gestion.persistent.dto;

public class ModulesAbonnementDTO {

    private Long tenantId;
    private String nomEntreprise;
    private Boolean moduleCommercialActif = true;
    private Boolean moduleComptabiliteActif = true;
    private Boolean moduleFiscaliteActif = true;
    private String offreActive; // "PACK_COMMERCIAL", "PACK_COMPTABILITE_FIDUCIAIRE", "ERP_INTEGRE_COMPLET"

    public ModulesAbonnementDTO() {}

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }

    public Boolean getModuleCommercialActif() { return moduleCommercialActif; }
    public void setModuleCommercialActif(Boolean moduleCommercialActif) { this.moduleCommercialActif = moduleCommercialActif; }

    public Boolean getModuleComptabiliteActif() { return moduleComptabiliteActif; }
    public void setModuleComptabiliteActif(Boolean moduleComptabiliteActif) { this.moduleComptabiliteActif = moduleComptabiliteActif; }

    public Boolean getModuleFiscaliteActif() { return moduleFiscaliteActif; }
    public void setModuleFiscaliteActif(Boolean moduleFiscaliteActif) { this.moduleFiscaliteActif = moduleFiscaliteActif; }

    public String getOffreActive() { return offreActive; }
    public void setOffreActive(String offreActive) { this.offreActive = offreActive; }
}
