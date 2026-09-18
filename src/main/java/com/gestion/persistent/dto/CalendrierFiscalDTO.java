package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendrierFiscalDTO {

    private Integer annee;
    private Long tenantId;
    private List<EcheanceFiscaleDTO> echeances = new ArrayList<>();

    public CalendrierFiscalDTO() {}

    public CalendrierFiscalDTO(Integer annee, Long tenantId) {
        this.annee = annee;
        this.tenantId = tenantId;
    }

    public static class EcheanceFiscaleDTO {
        private String code;
        private String titre;
        private String categorie; // "TVA", "IS", "CNSS", "LIASSE"
        private String periodicite; // "MENSUEL", "TRIMESTRIEL", "ANNUEL"
        private LocalDate dateLimite;
        private BigDecimal montantEstime = BigDecimal.ZERO;
        private BigDecimal montantRegle = BigDecimal.ZERO;
        private String statut; // "A_ECHOIR", "URGENT", "EN_RETARD", "REGLE"
        private long joursRestants;

        public EcheanceFiscaleDTO() {}

        public EcheanceFiscaleDTO(String code, String titre, String categorie, String periodicite,
                                  LocalDate dateLimite, BigDecimal montantEstime, BigDecimal montantRegle,
                                  String statut, long joursRestants) {
            this.code = code;
            this.titre = titre;
            this.categorie = categorie;
            this.periodicite = periodicite;
            this.dateLimite = dateLimite;
            this.montantEstime = montantEstime != null ? montantEstime : BigDecimal.ZERO;
            this.montantRegle = montantRegle != null ? montantRegle : BigDecimal.ZERO;
            this.statut = statut;
            this.joursRestants = joursRestants;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getCategorie() { return categorie; }
        public void setCategorie(String categorie) { this.categorie = categorie; }

        public String getPeriodicite() { return periodicite; }
        public void setPeriodicite(String periodicite) { this.periodicite = periodicite; }

        public LocalDate getDateLimite() { return dateLimite; }
        public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }

        public BigDecimal getMontantEstime() { return montantEstime; }
        public void setMontantEstime(BigDecimal montantEstime) { this.montantEstime = montantEstime; }

        public BigDecimal getMontantRegle() { return montantRegle; }
        public void setMontantRegle(BigDecimal montantRegle) { this.montantRegle = montantRegle; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public long getJoursRestants() { return joursRestants; }
        public void setJoursRestants(long joursRestants) { this.joursRestants = joursRestants; }
    }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public List<EcheanceFiscaleDTO> getEcheances() { return echeances; }
    public void setEcheances(List<EcheanceFiscaleDTO> echeances) { this.echeances = echeances; }
}
