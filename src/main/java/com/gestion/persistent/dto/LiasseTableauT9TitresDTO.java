package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT9TitresDTO {

    private String titre = "TABLEAU 9 : TITRES DE PARTICIPATION ET AUTRES TITRES IMMOBILISÉS";
    private Integer annee;
    private List<LigneTitreParticipationDTO> lignes = new ArrayList<>();

    private BigDecimal totalPrixAcquisition = BigDecimal.ZERO;
    private BigDecimal totalValeurComptableNette = BigDecimal.ZERO;
    private BigDecimal totalDividendesEncaisses = BigDecimal.ZERO;

    public LiasseTableauT9TitresDTO() {}

    public static class LigneTitreParticipationDTO {
        private String raisonSociale;
        private String secteurActivite;
        private BigDecimal capitalSocial = BigDecimal.ZERO;
        private BigDecimal partCapitalPourcentage = BigDecimal.ZERO;
        private BigDecimal prixAcquisitionGlobal = BigDecimal.ZERO;
        private BigDecimal valeurComptableNette = BigDecimal.ZERO;
        private BigDecimal dividendesEncaisses = BigDecimal.ZERO;

        public LigneTitreParticipationDTO() {}

        public LigneTitreParticipationDTO(String raisonSociale, String secteurActivite, BigDecimal capitalSocial,
                                          BigDecimal partCapital, BigDecimal prixAcq, BigDecimal vnc, BigDecimal dividendes) {
            this.raisonSociale = raisonSociale;
            this.secteurActivite = secteurActivite;
            this.capitalSocial = capitalSocial != null ? capitalSocial : BigDecimal.ZERO;
            this.partCapitalPourcentage = partCapital != null ? partCapital : BigDecimal.ZERO;
            this.prixAcquisitionGlobal = prixAcq != null ? prixAcq : BigDecimal.ZERO;
            this.valeurComptableNette = vnc != null ? vnc : BigDecimal.ZERO;
            this.dividendesEncaisses = dividendes != null ? dividendes : BigDecimal.ZERO;
        }

        public String getRaisonSociale() { return raisonSociale; }
        public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

        public String getSecteurActivite() { return secteurActivite; }
        public void setSecteurActivite(String secteurActivite) { this.secteurActivite = secteurActivite; }

        public BigDecimal getCapitalSocial() { return capitalSocial; }
        public void setCapitalSocial(BigDecimal capitalSocial) { this.capitalSocial = capitalSocial; }

        public BigDecimal getPartCapitalPourcentage() { return partCapitalPourcentage; }
        public void setPartCapitalPourcentage(BigDecimal partCapitalPourcentage) { this.partCapitalPourcentage = partCapitalPourcentage; }

        public BigDecimal getPrixAcquisitionGlobal() { return prixAcquisitionGlobal; }
        public void setPrixAcquisitionGlobal(BigDecimal prixAcquisitionGlobal) { this.prixAcquisitionGlobal = prixAcquisitionGlobal; }

        public BigDecimal getValeurComptableNette() { return valeurComptableNette; }
        public void setValeurComptableNette(BigDecimal valeurComptableNette) { this.valeurComptableNette = valeurComptableNette; }

        public BigDecimal getDividendesEncaisses() { return dividendesEncaisses; }
        public void setDividendesEncaisses(BigDecimal dividendesEncaisses) { this.dividendesEncaisses = dividendesEncaisses; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneTitreParticipationDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneTitreParticipationDTO> lignes) { this.lignes = lignes; }

    public BigDecimal getTotalPrixAcquisition() { return totalPrixAcquisition; }
    public void setTotalPrixAcquisition(BigDecimal totalPrixAcquisition) { this.totalPrixAcquisition = totalPrixAcquisition; }

    public BigDecimal getTotalValeurComptableNette() { return totalValeurComptableNette; }
    public void setTotalValeurComptableNette(BigDecimal totalValeurComptableNette) { this.totalValeurComptableNette = totalValeurComptableNette; }

    public BigDecimal getTotalDividendesEncaisses() { return totalDividendesEncaisses; }
    public void setTotalDividendesEncaisses(BigDecimal totalDividendesEncaisses) { this.totalDividendesEncaisses = totalDividendesEncaisses; }
}
