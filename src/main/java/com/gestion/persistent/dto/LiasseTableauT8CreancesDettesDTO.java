package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT8CreancesDettesDTO {

    private String titre = "TABLEAU 8 : TABLEAU DES CRÉANCES ET DES DETTES";
    private Integer annee;

    private List<LigneEcheanceDTO> creances = new ArrayList<>();
    private BigDecimal totalCreances = BigDecimal.ZERO;
    private BigDecimal totalCreancesPlusUnAn = BigDecimal.ZERO;
    private BigDecimal totalCreancesMoinsUnAn = BigDecimal.ZERO;

    private List<LigneEcheanceDTO> dettes = new ArrayList<>();
    private BigDecimal totalDettes = BigDecimal.ZERO;
    private BigDecimal totalDettesPlusUnAn = BigDecimal.ZERO;
    private BigDecimal totalDettesMoinsUnAn = BigDecimal.ZERO;

    public LiasseTableauT8CreancesDettesDTO() {}

    public static class LigneEcheanceDTO {
        private String codePoste;
        private String libelle;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal plusUnAn = BigDecimal.ZERO;
        private BigDecimal moinsUnAn = BigDecimal.ZERO;

        public LigneEcheanceDTO() {}

        public LigneEcheanceDTO(String codePoste, String libelle, BigDecimal total, BigDecimal plusUnAn, BigDecimal moinsUnAn) {
            this.codePoste = codePoste;
            this.libelle = libelle;
            this.total = total != null ? total : BigDecimal.ZERO;
            this.plusUnAn = plusUnAn != null ? plusUnAn : BigDecimal.ZERO;
            this.moinsUnAn = moinsUnAn != null ? moinsUnAn : BigDecimal.ZERO;
        }

        public String getCodePoste() { return codePoste; }
        public void setCodePoste(String codePoste) { this.codePoste = codePoste; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public BigDecimal getPlusUnAn() { return plusUnAn; }
        public void setPlusUnAn(BigDecimal plusUnAn) { this.plusUnAn = plusUnAn; }

        public BigDecimal getMoinsUnAn() { return moinsUnAn; }
        public void setMoinsUnAn(BigDecimal moinsUnAn) { this.moinsUnAn = moinsUnAn; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneEcheanceDTO> getCreances() { return creances; }
    public void setCreances(List<LigneEcheanceDTO> creances) { this.creances = creances; }

    public BigDecimal getTotalCreances() { return totalCreances; }
    public void setTotalCreances(BigDecimal totalCreances) { this.totalCreances = totalCreances; }

    public BigDecimal getTotalCreancesPlusUnAn() { return totalCreancesPlusUnAn; }
    public void setTotalCreancesPlusUnAn(BigDecimal totalCreancesPlusUnAn) { this.totalCreancesPlusUnAn = totalCreancesPlusUnAn; }

    public BigDecimal getTotalCreancesMoinsUnAn() { return totalCreancesMoinsUnAn; }
    public void setTotalCreancesMoinsUnAn(BigDecimal totalCreancesMoinsUnAn) { this.totalCreancesMoinsUnAn = totalCreancesMoinsUnAn; }

    public List<LigneEcheanceDTO> getDettes() { return dettes; }
    public void setDettes(List<LigneEcheanceDTO> dettes) { this.dettes = dettes; }

    public BigDecimal getTotalDettes() { return totalDettes; }
    public void setTotalDettes(BigDecimal totalDettes) { this.totalDettes = totalDettes; }

    public BigDecimal getTotalDettesPlusUnAn() { return totalDettesPlusUnAn; }
    public void setTotalDettesPlusUnAn(BigDecimal totalDettesPlusUnAn) { this.totalDettesPlusUnAn = totalDettesPlusUnAn; }

    public BigDecimal getTotalDettesMoinsUnAn() { return totalDettesMoinsUnAn; }
    public void setTotalDettesMoinsUnAn(BigDecimal totalDettesMoinsUnAn) { this.totalDettesMoinsUnAn = totalDettesMoinsUnAn; }
}
