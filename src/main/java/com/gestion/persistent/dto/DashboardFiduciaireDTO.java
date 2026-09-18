package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardFiduciaireDTO {
    private long totalSocietes;
    private long societesActives;
    private long echeancesAFaire;
    private long echeancesEnRetard;
    private List<EcheanceFiscaleDTO> prochainesEcheances = new ArrayList<>();
    private List<SyntheseDossierDTO> synthesesDossiers = new ArrayList<>();

    public static class SyntheseDossierDTO {
        private Long societeId;
        private String code;
        private String raisonSociale;
        private String ice;
        private String formeJuridique;
        private String responsable;
        private Integer exercice;
        private String periodiciteTva;
        private long alertesRetard;

        public SyntheseDossierDTO() {}

        public Long getSocieteId() { return societeId; }
        public void setSocieteId(Long societeId) { this.societeId = societeId; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getRaisonSociale() { return raisonSociale; }
        public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

        public String getIce() { return ice; }
        public void setIce(String ice) { this.ice = ice; }

        public String getFormeJuridique() { return formeJuridique; }
        public void setFormeJuridique(String formeJuridique) { this.formeJuridique = formeJuridique; }

        public String getResponsable() { return responsable; }
        public void setResponsable(String responsable) { this.responsable = responsable; }

        public Integer getExercice() { return exercice; }
        public void setExercice(Integer exercice) { this.exercice = exercice; }

        public String getPeriodiciteTva() { return periodiciteTva; }
        public void setPeriodiciteTva(String periodiciteTva) { this.periodiciteTva = periodiciteTva; }

        public long getAlertesRetard() { return alertesRetard; }
        public void setAlertesRetard(long alertesRetard) { this.alertesRetard = alertesRetard; }
    }

    public DashboardFiduciaireDTO() {}

    public long getTotalSocietes() { return totalSocietes; }
    public void setTotalSocietes(long totalSocietes) { this.totalSocietes = totalSocietes; }

    public long getSocietesActives() { return societesActives; }
    public void setSocietesActives(long societesActives) { this.societesActives = societesActives; }

    public long getEcheancesAFaire() { return echeancesAFaire; }
    public void setEcheancesAFaire(long echeancesAFaire) { this.echeancesAFaire = echeancesAFaire; }

    public long getEcheancesEnRetard() { return echeancesEnRetard; }
    public void setEcheancesEnRetard(long echeancesEnRetard) { this.echeancesEnRetard = echeancesEnRetard; }

    public List<EcheanceFiscaleDTO> getProchainesEcheances() { return prochainesEcheances; }
    public void setProchainesEcheances(List<EcheanceFiscaleDTO> prochainesEcheances) { this.prochainesEcheances = prochainesEcheances; }

    public List<SyntheseDossierDTO> getSynthesesDossiers() { return synthesesDossiers; }
    public void setSynthesesDossiers(List<SyntheseDossierDTO> synthesesDossiers) { this.synthesesDossiers = synthesesDossiers; }
}
