package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT7ProvisionsDTO {

    private String titre = "TABLEAU 7 : TABLEAU DES PROVISIONS";
    private Integer annee;
    private List<LigneProvisionDTO> lignes = new ArrayList<>();

    private BigDecimal totalDebut = BigDecimal.ZERO;
    private BigDecimal totalDotationsExploitation = BigDecimal.ZERO;
    private BigDecimal totalDotationsFinancieres = BigDecimal.ZERO;
    private BigDecimal totalDotationsNonCourantes = BigDecimal.ZERO;
    private BigDecimal totalReprisesExploitation = BigDecimal.ZERO;
    private BigDecimal totalReprisesFinancieres = BigDecimal.ZERO;
    private BigDecimal totalReprisesNonCourantes = BigDecimal.ZERO;
    private BigDecimal totalFin = BigDecimal.ZERO;

    public LiasseTableauT7ProvisionsDTO() {}

    public static class LigneProvisionDTO {
        private String codePoste;
        private String nature;
        private BigDecimal montantDebut = BigDecimal.ZERO;
        private BigDecimal dotationsExploitation = BigDecimal.ZERO;
        private BigDecimal dotationsFinancieres = BigDecimal.ZERO;
        private BigDecimal dotationsNonCourantes = BigDecimal.ZERO;
        private BigDecimal reprisesExploitation = BigDecimal.ZERO;
        private BigDecimal reprisesFinancieres = BigDecimal.ZERO;
        private BigDecimal reprisesNonCourantes = BigDecimal.ZERO;
        private BigDecimal montantFin = BigDecimal.ZERO;

        public LigneProvisionDTO() {}

        public LigneProvisionDTO(String codePoste, String nature, BigDecimal montantDebut,
                                 BigDecimal dotExp, BigDecimal dotFin, BigDecimal dotNc,
                                 BigDecimal repExp, BigDecimal repFin, BigDecimal repNc,
                                 BigDecimal montantFin) {
            this.codePoste = codePoste;
            this.nature = nature;
            this.montantDebut = montantDebut != null ? montantDebut : BigDecimal.ZERO;
            this.dotationsExploitation = dotExp != null ? dotExp : BigDecimal.ZERO;
            this.dotationsFinancieres = dotFin != null ? dotFin : BigDecimal.ZERO;
            this.dotationsNonCourantes = dotNc != null ? dotNc : BigDecimal.ZERO;
            this.reprisesExploitation = repExp != null ? repExp : BigDecimal.ZERO;
            this.reprisesFinancieres = repFin != null ? repFin : BigDecimal.ZERO;
            this.reprisesNonCourantes = repNc != null ? repNc : BigDecimal.ZERO;
            this.montantFin = montantFin != null ? montantFin : BigDecimal.ZERO;
        }

        public String getCodePoste() { return codePoste; }
        public void setCodePoste(String codePoste) { this.codePoste = codePoste; }

        public String getNature() { return nature; }
        public void setNature(String nature) { this.nature = nature; }

        public BigDecimal getMontantDebut() { return montantDebut; }
        public void setMontantDebut(BigDecimal montantDebut) { this.montantDebut = montantDebut; }

        public BigDecimal getDotationsExploitation() { return dotationsExploitation; }
        public void setDotationsExploitation(BigDecimal dotationsExploitation) { this.dotationsExploitation = dotationsExploitation; }

        public BigDecimal getDotationsFinancieres() { return dotationsFinancieres; }
        public void setDotationsFinancieres(BigDecimal dotationsFinancieres) { this.dotationsFinancieres = dotationsFinancieres; }

        public BigDecimal getDotationsNonCourantes() { return dotationsNonCourantes; }
        public void setDotationsNonCourantes(BigDecimal dotationsNonCourantes) { this.dotationsNonCourantes = dotationsNonCourantes; }

        public BigDecimal getReprisesExploitation() { return reprisesExploitation; }
        public void setReprisesExploitation(BigDecimal reprisesExploitation) { this.reprisesExploitation = reprisesExploitation; }

        public BigDecimal getReprisesFinancieres() { return reprisesFinancieres; }
        public void setReprisesFinancieres(BigDecimal reprisesFinancieres) { this.reprisesFinancieres = reprisesFinancieres; }

        public BigDecimal getReprisesNonCourantes() { return reprisesNonCourantes; }
        public void setReprisesNonCourantes(BigDecimal reprisesNonCourantes) { this.reprisesNonCourantes = reprisesNonCourantes; }

        public BigDecimal getMontantFin() { return montantFin; }
        public void setMontantFin(BigDecimal montantFin) { this.montantFin = montantFin; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneProvisionDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneProvisionDTO> lignes) { this.lignes = lignes; }

    public BigDecimal getTotalDebut() { return totalDebut; }
    public void setTotalDebut(BigDecimal totalDebut) { this.totalDebut = totalDebut; }

    public BigDecimal getTotalDotationsExploitation() { return totalDotationsExploitation; }
    public void setTotalDotationsExploitation(BigDecimal totalDotationsExploitation) { this.totalDotationsExploitation = totalDotationsExploitation; }

    public BigDecimal getTotalDotationsFinancieres() { return totalDotationsFinancieres; }
    public void setTotalDotationsFinancieres(BigDecimal totalDotationsFinancieres) { this.totalDotationsFinancieres = totalDotationsFinancieres; }

    public BigDecimal getTotalDotationsNonCourantes() { return totalDotationsNonCourantes; }
    public void setTotalDotationsNonCourantes(BigDecimal totalDotationsNonCourantes) { this.totalDotationsNonCourantes = totalDotationsNonCourantes; }

    public BigDecimal getTotalReprisesExploitation() { return totalReprisesExploitation; }
    public void setTotalReprisesExploitation(BigDecimal totalReprisesExploitation) { this.totalReprisesExploitation = totalReprisesExploitation; }

    public BigDecimal getTotalReprisesFinancieres() { return totalReprisesFinancieres; }
    public void setTotalReprisesFinancieres(BigDecimal totalReprisesFinancieres) { this.totalReprisesFinancieres = totalReprisesFinancieres; }

    public BigDecimal getTotalReprisesNonCourantes() { return totalReprisesNonCourantes; }
    public void setTotalReprisesNonCourantes(BigDecimal totalReprisesNonCourantes) { this.totalReprisesNonCourantes = totalReprisesNonCourantes; }

    public BigDecimal getTotalFin() { return totalFin; }
    public void setTotalFin(BigDecimal totalFin) { this.totalFin = totalFin; }
}
