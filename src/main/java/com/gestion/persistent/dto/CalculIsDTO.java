package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CalculIsDTO {

    private Integer anneeFiscale;
    private Long tenantId;

    // Passage du résultat comptable au résultat fiscal
    private BigDecimal resultatComptableAvantImpot = BigDecimal.ZERO;
    private BigDecimal reintegrationsFiscales = BigDecimal.ZERO;
    private BigDecimal deductionsFiscales = BigDecimal.ZERO;
    private BigDecimal resultatFiscal = BigDecimal.ZERO;

    // Détail du calcul au barème progressif
    private BigDecimal isCalculeBareme = BigDecimal.ZERO;
    private List<TrancheDetailDTO> detailTranches = new ArrayList<>();

    // Cotisation Minimale (CM)
    private BigDecimal baseCotisationMinimale = BigDecimal.ZERO;
    private BigDecimal tauxCotisationMinimale = BigDecimal.ZERO;
    private BigDecimal cotisationMinimaleCalculee = BigDecimal.ZERO;
    private BigDecimal plancherCotisationMinimale = BigDecimal.ZERO;
    private BigDecimal cotisationMinimaleRetenue = BigDecimal.ZERO;

    // Impôt exigible retenu
    private BigDecimal impotExigible = BigDecimal.ZERO;
    private String natureImpotRetenu; // "IS_BAREME" ou "COTISATION_MINIMALE"
    private BigDecimal creditCotisationMinimale = BigDecimal.ZERO;

    // Rapprochement réel avec les acomptes versés
    private BigDecimal acomptesVerses = BigDecimal.ZERO;
    private BigDecimal reliquatAPayer = BigDecimal.ZERO;
    private BigDecimal excedentVersement = BigDecimal.ZERO;

    public CalculIsDTO() {}

    public static class TrancheDetailDTO {
        private String tranche;
        private BigDecimal baseImposable = BigDecimal.ZERO;
        private BigDecimal taux = BigDecimal.ZERO;
        private BigDecimal montantImpot = BigDecimal.ZERO;

        public TrancheDetailDTO() {}

        public TrancheDetailDTO(String tranche, BigDecimal baseImposable, BigDecimal taux, BigDecimal montantImpot) {
            this.tranche = tranche;
            this.baseImposable = baseImposable != null ? baseImposable : BigDecimal.ZERO;
            this.taux = taux != null ? taux : BigDecimal.ZERO;
            this.montantImpot = montantImpot != null ? montantImpot : BigDecimal.ZERO;
        }

        public String getTranche() { return tranche; }
        public void setTranche(String tranche) { this.tranche = tranche; }

        public BigDecimal getBaseImposable() { return baseImposable; }
        public void setBaseImposable(BigDecimal baseImposable) { this.baseImposable = baseImposable; }

        public BigDecimal getTaux() { return taux; }
        public void setTaux(BigDecimal taux) { this.taux = taux; }

        public BigDecimal getMontantImpot() { return montantImpot; }
        public void setMontantImpot(BigDecimal montantImpot) { this.montantImpot = montantImpot; }
    }

    public Integer getAnneeFiscale() { return anneeFiscale; }
    public void setAnneeFiscale(Integer anneeFiscale) { this.anneeFiscale = anneeFiscale; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public BigDecimal getResultatComptableAvantImpot() { return resultatComptableAvantImpot; }
    public void setResultatComptableAvantImpot(BigDecimal resultatComptableAvantImpot) { this.resultatComptableAvantImpot = resultatComptableAvantImpot; }

    public BigDecimal getReintegrationsFiscales() { return reintegrationsFiscales; }
    public void setReintegrationsFiscales(BigDecimal reintegrationsFiscales) { this.reintegrationsFiscales = reintegrationsFiscales; }

    public BigDecimal getDeductionsFiscales() { return deductionsFiscales; }
    public void setDeductionsFiscales(BigDecimal deductionsFiscales) { this.deductionsFiscales = deductionsFiscales; }

    public BigDecimal getResultatFiscal() { return resultatFiscal; }
    public void setResultatFiscal(BigDecimal resultatFiscal) { this.resultatFiscal = resultatFiscal; }

    public BigDecimal getIsCalculeBareme() { return isCalculeBareme; }
    public void setIsCalculeBareme(BigDecimal isCalculeBareme) { this.isCalculeBareme = isCalculeBareme; }

    public List<TrancheDetailDTO> getDetailTranches() { return detailTranches; }
    public void setDetailTranches(List<TrancheDetailDTO> detailTranches) { this.detailTranches = detailTranches; }

    public BigDecimal getBaseCotisationMinimale() { return baseCotisationMinimale; }
    public void setBaseCotisationMinimale(BigDecimal baseCotisationMinimale) { this.baseCotisationMinimale = baseCotisationMinimale; }

    public BigDecimal getTauxCotisationMinimale() { return tauxCotisationMinimale; }
    public void setTauxCotisationMinimale(BigDecimal tauxCotisationMinimale) { this.tauxCotisationMinimale = tauxCotisationMinimale; }

    public BigDecimal getCotisationMinimaleCalculee() { return cotisationMinimaleCalculee; }
    public void setCotisationMinimaleCalculee(BigDecimal cotisationMinimaleCalculee) { this.cotisationMinimaleCalculee = cotisationMinimaleCalculee; }

    public BigDecimal getPlancherCotisationMinimale() { return plancherCotisationMinimale; }
    public void setPlancherCotisationMinimale(BigDecimal plancherCotisationMinimale) { this.plancherCotisationMinimale = plancherCotisationMinimale; }

    public BigDecimal getCotisationMinimaleRetenue() { return cotisationMinimaleRetenue; }
    public void setCotisationMinimaleRetenue(BigDecimal cotisationMinimaleRetenue) { this.cotisationMinimaleRetenue = cotisationMinimaleRetenue; }

    public BigDecimal getImpotExigible() { return impotExigible; }
    public void setImpotExigible(BigDecimal impotExigible) { this.impotExigible = impotExigible; }

    public String getNatureImpotRetenu() { return natureImpotRetenu; }
    public void setNatureImpotRetenu(String natureImpotRetenu) { this.natureImpotRetenu = natureImpotRetenu; }

    public BigDecimal getCreditCotisationMinimale() { return creditCotisationMinimale; }
    public void setCreditCotisationMinimale(BigDecimal creditCotisationMinimale) { this.creditCotisationMinimale = creditCotisationMinimale; }

    public BigDecimal getAcomptesVerses() { return acomptesVerses; }
    public void setAcomptesVerses(BigDecimal acomptesVerses) { this.acomptesVerses = acomptesVerses; }

    public BigDecimal getReliquatAPayer() { return reliquatAPayer; }
    public void setReliquatAPayer(BigDecimal reliquatAPayer) { this.reliquatAPayer = reliquatAPayer; }

    public BigDecimal getExcedentVersement() { return excedentVersement; }
    public void setExcedentVersement(BigDecimal excedentVersement) { this.excedentVersement = excedentVersement; }
}
