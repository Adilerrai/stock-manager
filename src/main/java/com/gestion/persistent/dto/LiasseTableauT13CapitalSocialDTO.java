package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT13CapitalSocialDTO {

    private String titre = "TABLEAU 13 : RÉPARTITION DU CAPITAL SOCIAL";
    private Integer annee;
    private BigDecimal capitalSocialTotal = BigDecimal.ZERO;
    private BigDecimal valeurNominaleUnitaire = BigDecimal.ZERO;
    private Long nombreTotalTitres = 0L;
    private List<LigneAssocieDTO> associes = new ArrayList<>();

    public LiasseTableauT13CapitalSocialDTO() {}

    public static class LigneAssocieDTO {
        private String nomOuRaisonSociale;
        private String identifiantFiscalOuCin;
        private String adresse;
        private Long nombreTitres = 0L;
        private BigDecimal montantSouscrit = BigDecimal.ZERO;
        private BigDecimal montantLibere = BigDecimal.ZERO;
        private BigDecimal pourcentageDetention = BigDecimal.ZERO;

        public LigneAssocieDTO() {}

        public LigneAssocieDTO(String nom, String idFiscal, String adresse, Long nombreTitres,
                               BigDecimal montantSouscrit, BigDecimal montantLibere, BigDecimal pct) {
            this.nomOuRaisonSociale = nom;
            this.identifiantFiscalOuCin = idFiscal;
            this.adresse = adresse;
            this.nombreTitres = nombreTitres;
            this.montantSouscrit = montantSouscrit != null ? montantSouscrit : BigDecimal.ZERO;
            this.montantLibere = montantLibere != null ? montantLibere : BigDecimal.ZERO;
            this.pourcentageDetention = pct != null ? pct : BigDecimal.ZERO;
        }

        public String getNomOuRaisonSociale() { return nomOuRaisonSociale; }
        public void setNomOuRaisonSociale(String nomOuRaisonSociale) { this.nomOuRaisonSociale = nomOuRaisonSociale; }

        public String getIdentifiantFiscalOuCin() { return identifiantFiscalOuCin; }
        public void setIdentifiantFiscalOuCin(String identifiantFiscalOuCin) { this.identifiantFiscalOuCin = identifiantFiscalOuCin; }

        public String getAdresse() { return adresse; }
        public void setAdresse(String adresse) { this.adresse = adresse; }

        public Long getNombreTitres() { return nombreTitres; }
        public void setNombreTitres(Long nombreTitres) { this.nombreTitres = nombreTitres; }

        public BigDecimal getMontantSouscrit() { return montantSouscrit; }
        public void setMontantSouscrit(BigDecimal montantSouscrit) { this.montantSouscrit = montantSouscrit; }

        public BigDecimal getMontantLibere() { return montantLibere; }
        public void setMontantLibere(BigDecimal montantLibere) { this.montantLibere = montantLibere; }

        public BigDecimal getPourcentageDetention() { return pourcentageDetention; }
        public void setPourcentageDetention(BigDecimal pourcentageDetention) { this.pourcentageDetention = pourcentageDetention; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public BigDecimal getCapitalSocialTotal() { return capitalSocialTotal; }
    public void setCapitalSocialTotal(BigDecimal capitalSocialTotal) { this.capitalSocialTotal = capitalSocialTotal; }

    public BigDecimal getValeurNominaleUnitaire() { return valeurNominaleUnitaire; }
    public void setValeurNominaleUnitaire(BigDecimal valeurNominaleUnitaire) { this.valeurNominaleUnitaire = valeurNominaleUnitaire; }

    public Long getNombreTotalTitres() { return nombreTotalTitres; }
    public void setNombreTotalTitres(Long nombreTotalTitres) { this.nombreTotalTitres = nombreTotalTitres; }

    public List<LigneAssocieDTO> getAssocies() { return associes; }
    public void setAssocies(List<LigneAssocieDTO> associes) { this.associes = associes; }
}
