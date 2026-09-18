package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT11DTO {

    private String titre = "TABLEAU 11 (A4) : TABLEAU DES AMORTISSEMENTS";
    private Integer annee;
    private List<LigneAmortissementA4DTO> lignes = new ArrayList<>();

    private BigDecimal totalCumulDebut = BigDecimal.ZERO;
    private BigDecimal totalDotations = BigDecimal.ZERO;
    private BigDecimal totalAmortissementsSorties = BigDecimal.ZERO;
    private BigDecimal totalCumulFin = BigDecimal.ZERO;

    public LiasseTableauT11DTO() {}

    public static class LigneAmortissementA4DTO {
        private String codePoste;
        private String nature;
        private BigDecimal cumulDebut = BigDecimal.ZERO;
        private BigDecimal dotationsExercice = BigDecimal.ZERO;
        private BigDecimal amortissementsSorties = BigDecimal.ZERO;
        private BigDecimal cumulFin = BigDecimal.ZERO;

        public LigneAmortissementA4DTO() {}

        public LigneAmortissementA4DTO(String codePoste, String nature, BigDecimal cumulDebut,
                                      BigDecimal dotations, BigDecimal sorties, BigDecimal cumulFin) {
            this.codePoste = codePoste;
            this.nature = nature;
            this.cumulDebut = cumulDebut != null ? cumulDebut : BigDecimal.ZERO;
            this.dotationsExercice = dotations != null ? dotations : BigDecimal.ZERO;
            this.amortissementsSorties = sorties != null ? sorties : BigDecimal.ZERO;
            this.cumulFin = cumulFin != null ? cumulFin : BigDecimal.ZERO;
        }

        public String getCodePoste() { return codePoste; }
        public void setCodePoste(String codePoste) { this.codePoste = codePoste; }

        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }

        public BigDecimal getCumulDebut() { return cumulDebut; }
        public void setCumulDebut(BigDecimal cumulDebut) { this.cumulDebut = cumulDebut; }

        public BigDecimal getDotationsExercice() { return dotationsExercice; }
        public void setDotationsExercice(BigDecimal dotationsExercice) { this.dotationsExercice = dotationsExercice; }

        public BigDecimal getAmortissementsSorties() { return amortissementsSorties; }
        public void setAmortissementsSorties(BigDecimal amortissementsSorties) { this.amortissementsSorties = amortissementsSorties; }

        public BigDecimal getCumulFin() { return cumulFin; }
        public void setCumulFin(BigDecimal cumulFin) { this.cumulFin = cumulFin; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneAmortissementA4DTO> getLignes() { return lignes; }
    public void setLignes(List<LigneAmortissementA4DTO> lignes) { this.lignes = lignes; }

    public BigDecimal getTotalCumulDebut() { return totalCumulDebut; }
    public void setTotalCumulDebut(BigDecimal totalCumulDebut) { this.totalCumulDebut = totalCumulDebut; }

    public BigDecimal getTotalDotations() { return totalDotations; }
    public void setTotalDotations(BigDecimal totalDotations) { this.totalDotations = totalDotations; }

    public BigDecimal getTotalAmortissementsSorties() { return totalAmortissementsSorties; }
    public void setTotalAmortissementsSorties(BigDecimal totalAmortissementsSorties) { this.totalAmortissementsSorties = totalAmortissementsSorties; }

    public BigDecimal getTotalCumulFin() { return totalCumulFin; }
    public void setTotalCumulFin(BigDecimal totalCumulFin) { this.totalCumulFin = totalCumulFin; }
}
