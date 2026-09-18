package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT16ResultatFiscalDTO {

    private String titre = "TABLEAU 16 : DÉTERMINATION DU RÉSULTAT FISCAL (PASSAGE COMPTABLE AU FISCAL)";
    private Integer annee;

    // 1. Résultat comptable
    private BigDecimal resultatNetComptable = BigDecimal.ZERO;
    private boolean beneficeComptable = true;

    // 2. Réintégrations fiscales
    private List<LigneReconciliationFiscaleDTO> reintegrations = new ArrayList<>();
    private BigDecimal totalReintegrations = BigDecimal.ZERO;

    // 3. Déductions fiscales
    private List<LigneReconciliationFiscaleDTO> deductions = new ArrayList<>();
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // 4. Résultat fiscal brut
    private BigDecimal resultatFiscalBrut = BigDecimal.ZERO;

    // 5. Reports déficitaires
    private BigDecimal deficitsExericesAnterieurs = BigDecimal.ZERO;

    // 6. Résultat fiscal net
    private BigDecimal resultatFiscalNet = BigDecimal.ZERO;
    private boolean beneficeFiscal = true;

    public LiasseTableauT16ResultatFiscalDTO() {}

    public static class LigneReconciliationFiscaleDTO {
        private String motif;
        private BigDecimal montant = BigDecimal.ZERO;

        public LigneReconciliationFiscaleDTO() {}

        public LigneReconciliationFiscaleDTO(String motif, BigDecimal montant) {
            this.motif = motif;
            this.montant = montant != null ? montant : BigDecimal.ZERO;
        }

        public String getMotif() { return motif; }
        public void setMotif(String motif) { this.motif = motif; }

        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getResultatNetComptable() { return resultatNetComptable; }
    public void setResultatNetComptable(BigDecimal resultatNetComptable) { this.resultatNetComptable = resultatNetComptable; }

    public boolean isBeneficeComptable() { return beneficeComptable; }
    public void setBeneficeComptable(boolean beneficeComptable) { this.beneficeComptable = beneficeComptable; }

    public List<LigneReconciliationFiscaleDTO> getReintegrations() { return reintegrations; }
    public void setReintegrations(List<LigneReconciliationFiscaleDTO> reintegrations) { this.reintegrations = reintegrations; }

    public BigDecimal getTotalReintegrations() { return totalReintegrations; }
    public void setTotalReintegrations(BigDecimal totalReintegrations) { this.totalReintegrations = totalReintegrations; }

    public List<LigneReconciliationFiscaleDTO> getDeductions() { return deductions; }
    public void setDeductions(List<LigneReconciliationFiscaleDTO> deductions) { this.deductions = deductions; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }

    public BigDecimal getResultatFiscalBrut() { return resultatFiscalBrut; }
    public void setResultatFiscalBrut(BigDecimal resultatFiscalBrut) { this.resultatFiscalBrut = resultatFiscalBrut; }

    public BigDecimal getDeficitsExericesAnterieurs() { return deficitsExericesAnterieurs; }
    public void setDeficitsExericesAnterieurs(BigDecimal deficitsExericesAnterieurs) { this.deficitsExericesAnterieurs = deficitsExericesAnterieurs; }

    public BigDecimal getResultatFiscalNet() { return resultatFiscalNet; }
    public void setResultatFiscalNet(BigDecimal resultatFiscalNet) { this.resultatFiscalNet = resultatFiscalNet; }

    public boolean isBeneficeFiscal() { return beneficeFiscal; }
    public void setBeneficeFiscal(boolean beneficeFiscal) { this.beneficeFiscal = beneficeFiscal; }
}
