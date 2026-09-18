package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT10DTO {

    private String titre = "TABLEAU 10 (A3) : TABLEAU DES IMMOBILISATIONS AUTRES QUE FINANCIÈRES";
    private Integer annee;
    private List<LigneImmobilisationA3DTO> lignes = new ArrayList<>();

    private BigDecimal totalBrutDebut = BigDecimal.ZERO;
    private BigDecimal totalAcquisitions = BigDecimal.ZERO;
    private BigDecimal totalProductionInterne = BigDecimal.ZERO;
    private BigDecimal totalVirementsAugm = BigDecimal.ZERO;
    private BigDecimal totalCessions = BigDecimal.ZERO;
    private BigDecimal totalRetraits = BigDecimal.ZERO;
    private BigDecimal totalVirementsDim = BigDecimal.ZERO;
    private BigDecimal totalBrutFin = BigDecimal.ZERO;

    public LiasseTableauT10DTO() {}

    public static class LigneImmobilisationA3DTO {
        private String codePoste;
        private String nature;
        private BigDecimal montantBrutDebut = BigDecimal.ZERO;
        private BigDecimal acquisitions = BigDecimal.ZERO;
        private BigDecimal productionInterne = BigDecimal.ZERO;
        private BigDecimal virementsAugmentation = BigDecimal.ZERO;
        private BigDecimal cessions = BigDecimal.ZERO;
        private BigDecimal retraits = BigDecimal.ZERO;
        private BigDecimal virementsDiminution = BigDecimal.ZERO;
        private BigDecimal montantBrutFin = BigDecimal.ZERO;

        public LigneImmobilisationA3DTO() {}

        public LigneImmobilisationA3DTO(String codePoste, String nature, BigDecimal brutDebut,
                                       BigDecimal acq, BigDecimal cessions, BigDecimal retraits,
                                       BigDecimal brutFin) {
            this.codePoste = codePoste;
            this.nature = nature;
            this.montantBrutDebut = brutDebut != null ? brutDebut : BigDecimal.ZERO;
            this.acquisitions = acq != null ? acq : BigDecimal.ZERO;
            this.cessions = cessions != null ? cessions : BigDecimal.ZERO;
            this.retraits = retraits != null ? retraits : BigDecimal.ZERO;
            this.montantBrutFin = brutFin != null ? brutFin : BigDecimal.ZERO;
        }

        public String getCodePoste() { return codePoste; }
        public void setCodePoste(String codePoste) { this.codePoste = codePoste; }

        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }

        public BigDecimal getMontantBrutDebut() { return montantBrutDebut; }
        public void setMontantBrutDebut(BigDecimal montantBrutDebut) { this.montantBrutDebut = montantBrutDebut; }

        public BigDecimal getAcquisitions() { return acquisitions; }
        public void setAcquisitions(BigDecimal acquisitions) { this.acquisitions = acquisitions; }

        public BigDecimal getProductionInterne() { return productionInterne; }
        public void setProductionInterne(BigDecimal productionInterne) { this.productionInterne = productionInterne; }

        public BigDecimal getVirementsAugmentation() { return virementsAugmentation; }
        public void setVirementsAugmentation(BigDecimal virementsAugmentation) { this.virementsAugmentation = virementsAugmentation; }

        public BigDecimal getCessions() { return cessions; }
        public void setCessions(BigDecimal cessions) { this.cessions = cessions; }

        public BigDecimal getRetraits() { return retraits; }
        public void setRetraits(BigDecimal retraits) { this.retraits = retraits; }

        public BigDecimal getVirementsDiminution() { return virementsDiminution; }
        public void setVirementsDiminution(BigDecimal virementsDiminution) { this.virementsDiminution = virementsDiminution; }

        public BigDecimal getMontantBrutFin() { return montantBrutFin; }
        public void setMontantBrutFin(BigDecimal montantBrutFin) { this.montantBrutFin = montantBrutFin; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneImmobilisationA3DTO> getLignes() { return lignes; }
    public void setLignes(List<LigneImmobilisationA3DTO> lignes) { this.lignes = lignes; }

    public BigDecimal getTotalBrutDebut() { return totalBrutDebut; }
    public void setTotalBrutDebut(BigDecimal totalBrutDebut) { this.totalBrutDebut = totalBrutDebut; }

    public BigDecimal getTotalAcquisitions() { return totalAcquisitions; }
    public void setTotalAcquisitions(BigDecimal totalAcquisitions) { this.totalAcquisitions = totalAcquisitions; }

    public BigDecimal getTotalProductionInterne() { return totalProductionInterne; }
    public void setTotalProductionInterne(BigDecimal totalProductionInterne) { this.totalProductionInterne = totalProductionInterne; }

    public BigDecimal getTotalVirementsAugm() { return totalVirementsAugm; }
    public void setTotalVirementsAugm(BigDecimal totalVirementsAugm) { this.totalVirementsAugm = totalVirementsAugm; }

    public BigDecimal getTotalCessions() { return totalCessions; }
    public void setTotalCessions(BigDecimal totalCessions) { this.totalCessions = totalCessions; }

    public BigDecimal getTotalRetraits() { return totalRetraits; }
    public void setTotalRetraits(BigDecimal totalRetraits) { this.totalRetraits = totalRetraits; }

    public BigDecimal getTotalVirementsDim() { return totalVirementsDim; }
    public void setTotalVirementsDim(BigDecimal totalVirementsDim) { this.totalVirementsDim = totalVirementsDim; }

    public BigDecimal getTotalBrutFin() { return totalBrutFin; }
    public void setTotalBrutFin(BigDecimal totalBrutFin) { this.totalBrutFin = totalBrutFin; }
}
