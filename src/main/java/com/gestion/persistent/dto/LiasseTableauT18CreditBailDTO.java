package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT18CreditBailDTO {

    private String titre = "TABLEAU 18 : TABLEAU DES BIENS EN CRÉDIT-BAIL (LEASING)";
    private Integer annee;
    private List<LigneCreditBailDTO> contrats = new ArrayList<>();

    private BigDecimal totalValeurEstimee = BigDecimal.ZERO;
    private BigDecimal totalRedevancesExercice = BigDecimal.ZERO;
    private BigDecimal totalRedevancesRestantes = BigDecimal.ZERO;
    private BigDecimal totalPrixAchatResiduel = BigDecimal.ZERO;

    public LiasseTableauT18CreditBailDTO() {}

    public static class LigneCreditBailDTO {
        private String designationBien;
        private LocalDate datePremiereEcheance;
        private Integer dureeMois = 36;
        private BigDecimal valeurEstimeeContrat = BigDecimal.ZERO;
        private BigDecimal redevancesExercicesAnterieurs = BigDecimal.ZERO;
        private BigDecimal redevancesExercice = BigDecimal.ZERO;
        private BigDecimal redevancesRestantes = BigDecimal.ZERO;
        private BigDecimal prixAchatResiduel = BigDecimal.ZERO;

        public LigneCreditBailDTO() {}

        public LigneCreditBailDTO(String designation, LocalDate datePrem, Integer duree,
                                  BigDecimal valEstimee, BigDecimal ant, BigDecimal exerc,
                                  BigDecimal rest, BigDecimal prixAchat) {
            this.designationBien = designation;
            this.datePremiereEcheance = datePrem;
            this.dureeMois = duree != null ? duree : 36;
            this.valeurEstimeeContrat = valEstimee != null ? valEstimee : BigDecimal.ZERO;
            this.redevancesExercicesAnterieurs = ant != null ? ant : BigDecimal.ZERO;
            this.redevancesExercice = exerc != null ? exerc : BigDecimal.ZERO;
            this.redevancesRestantes = rest != null ? rest : BigDecimal.ZERO;
            this.prixAchatResiduel = prixAchat != null ? prixAchat : BigDecimal.ZERO;
        }

        public String getDesignationBien() { return designationBien; }
        public void setDesignationBien(String designationBien) { this.designationBien = designationBien; }

        public LocalDate getDatePremiereEcheance() { return datePremiereEcheance; }
        public void setDatePremiereEcheance(LocalDate datePremiereEcheance) { this.datePremiereEcheance = datePremiereEcheance; }

        public Integer getDureeMois() { return dureeMois; }
        public void setDureeMois(Integer dureeMois) { this.dureeMois = dureeMois; }

        public BigDecimal getValeurEstimeeContrat() { return valeurEstimeeContrat; }
        public void setValeurEstimeeContrat(BigDecimal valeurEstimeeContrat) { this.valeurEstimeeContrat = valeurEstimeeContrat; }

        public BigDecimal getRedevancesExercicesAnterieurs() { return redevancesExercicesAnterieurs; }
        public void setRedevancesExercicesAnterieurs(BigDecimal redevancesExercicesAnterieurs) { this.redevancesExercicesAnterieurs = redevancesExercicesAnterieurs; }

        public BigDecimal getRedevancesExercice() { return redevancesExercice; }
        public void setRedevancesExercice(BigDecimal redevancesExercice) { this.redevancesExercice = redevancesExercice; }

        public BigDecimal getRedevancesRestantes() { return redevancesRestantes; }
        public void setRedevancesRestantes(BigDecimal redevancesRestantes) { this.redevancesRestantes = redevancesRestantes; }

        public BigDecimal getPrixAchatResiduel() { return prixAchatResiduel; }
        public void setPrixAchatResiduel(BigDecimal prixAchatResiduel) { this.prixAchatResiduel = prixAchatResiduel; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneCreditBailDTO> getContrats() { return contrats; }
    public void setContrats(List<LigneCreditBailDTO> contrats) { this.contrats = contrats; }

    public BigDecimal getTotalValeurEstimee() { return totalValeurEstimee; }
    public void setTotalValeurEstimee(BigDecimal totalValeurEstimee) { this.totalValeurEstimee = totalValeurEstimee; }

    public BigDecimal getTotalRedevancesExercice() { return totalRedevancesExercice; }
    public void setTotalRedevancesExercice(BigDecimal totalRedevancesExercice) { this.totalRedevancesExercice = totalRedevancesExercice; }

    public BigDecimal getTotalRedevancesRestantes() { return totalRedevancesRestantes; }
    public void setTotalRedevancesRestantes(BigDecimal totalRedevancesRestantes) { this.totalRedevancesRestantes = totalRedevancesRestantes; }

    public BigDecimal getTotalPrixAchatResiduel() { return totalPrixAchatResiduel; }
    public void setTotalPrixAchatResiduel(BigDecimal totalPrixAchatResiduel) { this.totalPrixAchatResiduel = totalPrixAchatResiduel; }
}
