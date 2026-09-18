package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT12PlusMoinsValuesDTO {

    private String titre = "TABLEAU 12 (T23) : PLUS OU MOINS-VALUES SUR CESSIONS OU RETRAITS D'IMMOBILISATIONS";
    private Integer annee;
    private List<LigneCessionImmoDTO> cessions = new ArrayList<>();

    private BigDecimal totalValeurBrute = BigDecimal.ZERO;
    private BigDecimal totalAmortissementsCumules = BigDecimal.ZERO;
    private BigDecimal totalVna = BigDecimal.ZERO;
    private BigDecimal totalPrixCession = BigDecimal.ZERO;
    private BigDecimal totalPlusValues = BigDecimal.ZERO;
    private BigDecimal totalMoinsValues = BigDecimal.ZERO;

    public LiasseTableauT12PlusMoinsValuesDTO() {}

    public static class LigneCessionImmoDTO {
        private LocalDate dateCession;
        private String comptePrincipal;
        private String designation;
        private BigDecimal valeurBrute = BigDecimal.ZERO;
        private BigDecimal amortissementsCumules = BigDecimal.ZERO;
        private BigDecimal valeurNetteAmortissements = BigDecimal.ZERO; // VNA
        private BigDecimal prixCession = BigDecimal.ZERO;
        private BigDecimal plusValue = BigDecimal.ZERO;
        private BigDecimal moinsValue = BigDecimal.ZERO;

        public LigneCessionImmoDTO() {}

        public LigneCessionImmoDTO(LocalDate dateCession, String comptePrincipal, String designation,
                                   BigDecimal valeurBrute, BigDecimal amortissementsCumules,
                                   BigDecimal vna, BigDecimal prixCession) {
            this.dateCession = dateCession;
            this.comptePrincipal = comptePrincipal;
            this.designation = designation;
            this.valeurBrute = valeurBrute != null ? valeurBrute : BigDecimal.ZERO;
            this.amortissementsCumules = amortissementsCumules != null ? amortissementsCumules : BigDecimal.ZERO;
            this.valeurNetteAmortissements = vna != null ? vna : BigDecimal.ZERO;
            this.prixCession = prixCession != null ? prixCession : BigDecimal.ZERO;

            BigDecimal diff = this.prixCession.subtract(this.valeurNetteAmortissements);
            if (diff.compareTo(BigDecimal.ZERO) >= 0) {
                this.plusValue = diff;
                this.moinsValue = BigDecimal.ZERO;
            } else {
                this.plusValue = BigDecimal.ZERO;
                this.moinsValue = diff.abs();
            }
        }

        public LocalDate getDateCession() { return dateCession; }
        public void setDateCession(LocalDate dateCession) { this.dateCession = dateCession; }

        public String getComptePrincipal() { return comptePrincipal; }
        public void setComptePrincipal(String comptePrincipal) { this.comptePrincipal = comptePrincipal; }

        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }

        public BigDecimal getValeurBrute() { return valeurBrute; }
        public void setValeurBrute(BigDecimal valeurBrute) { this.valeurBrute = valeurBrute; }

        public BigDecimal getAmortissementsCumules() { return amortissementsCumules; }
        public void setAmortissementsCumules(BigDecimal amortissementsCumules) { this.amortissementsCumules = amortissementsCumules; }

        public BigDecimal getValeurNetteAmortissements() { return valeurNetteAmortissements; }
        public void setValeurNetteAmortissements(BigDecimal valeurNetteAmortissements) { this.valeurNetteAmortissements = valeurNetteAmortissements; }

        public BigDecimal getPrixCession() { return prixCession; }
        public void setPrixCession(BigDecimal prixCession) { this.prixCession = prixCession; }

        public BigDecimal getPlusValue() { return plusValue; }
        public void setPlusValue(BigDecimal plusValue) { this.plusValue = plusValue; }

        public BigDecimal getMoinsValue() { return moinsValue; }
        public void setMoinsValue(BigDecimal moinsValue) { this.moinsValue = moinsValue; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneCessionImmoDTO> getCessions() { return cessions; }
    public void setCessions(List<LigneCessionImmoDTO> cessions) { this.cessions = cessions; }

    public BigDecimal getTotalValeurBrute() { return totalValeurBrute; }
    public void setTotalValeurBrute(BigDecimal totalValeurBrute) { this.totalValeurBrute = totalValeurBrute; }

    public BigDecimal getTotalAmortissementsCumules() { return totalAmortissementsCumules; }
    public void setTotalAmortissementsCumules(BigDecimal totalAmortissementsCumules) { this.totalAmortissementsCumules = totalAmortissementsCumules; }

    public BigDecimal getTotalVna() { return totalVna; }
    public void setTotalVna(BigDecimal totalVna) { this.totalVna = totalVna; }

    public BigDecimal getTotalPrixCession() { return totalPrixCession; }
    public void setTotalPrixCession(BigDecimal totalPrixCession) { this.totalPrixCession = totalPrixCession; }

    public BigDecimal getTotalPlusValues() { return totalPlusValues; }
    public void setTotalPlusValues(BigDecimal totalPlusValues) { this.totalPlusValues = totalPlusValues; }

    public BigDecimal getTotalMoinsValues() { return totalMoinsValues; }
    public void setTotalMoinsValues(BigDecimal totalMoinsValues) { this.totalMoinsValues = totalMoinsValues; }
}
