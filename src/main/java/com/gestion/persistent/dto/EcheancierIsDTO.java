package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EcheancierIsDTO {

    private Integer anneeFiscale;
    private Long tenantId;
    private BigDecimal impotReference = BigDecimal.ZERO;
    private BigDecimal totalAcomptesDus = BigDecimal.ZERO;
    private BigDecimal totalAcomptesVerses = BigDecimal.ZERO;
    private BigDecimal soldeRestantAcomptes = BigDecimal.ZERO;

    private List<AcompteItemDTO> acomptes = new ArrayList<>();

    // Régularisation finale (Reliquat) au 31 Mars N+1
    private LocalDate dateLimiteReliquat;
    private BigDecimal montantReliquat = BigDecimal.ZERO;
    private String statutReliquat; // "A_PAYER", "REGLE", "EXCEDENT_A_IMPUTER"

    public EcheancierIsDTO() {}

    public static class AcompteItemDTO {
        private int numero;
        private String libelle;
        private BigDecimal pourcentage = new BigDecimal("25.00");
        private LocalDate dateLimite;
        private BigDecimal montantDu = BigDecimal.ZERO;
        private BigDecimal montantRegle = BigDecimal.ZERO;
        private BigDecimal soldeAcompte = BigDecimal.ZERO;
        private String statut; // "REGLE", "PARTIEL", "A_PAYER", "EN_RETARD"
        private String referencePaiement;

        public AcompteItemDTO() {}

        public AcompteItemDTO(int numero, String libelle, BigDecimal pourcentage, LocalDate dateLimite, BigDecimal montantDu, BigDecimal montantRegle, String statut) {
            this.numero = numero;
            this.libelle = libelle;
            this.pourcentage = pourcentage != null ? pourcentage : new BigDecimal("25.00");
            this.dateLimite = dateLimite;
            this.montantDu = montantDu != null ? montantDu : BigDecimal.ZERO;
            this.montantRegle = montantRegle != null ? montantRegle : BigDecimal.ZERO;
            this.soldeAcompte = this.montantDu.subtract(this.montantRegle);
            this.statut = statut;
        }

        public int getNumero() { return numero; }
        public void setNumero(int numero) { this.numero = numero; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getPourcentage() { return pourcentage; }
        public void setPourcentage(BigDecimal pourcentage) { this.pourcentage = pourcentage; }

        public LocalDate getDateLimite() { return dateLimite; }
        public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }

        public BigDecimal getMontantDu() { return montantDu; }
        public void setMontantDu(BigDecimal montantDu) { this.montantDu = montantDu; }

        public BigDecimal getMontantRegle() { return montantRegle; }
        public void setMontantRegle(BigDecimal montantRegle) { this.montantRegle = montantRegle; }

        public BigDecimal getSoldeAcompte() { return soldeAcompte; }
        public void setSoldeAcompte(BigDecimal soldeAcompte) { this.soldeAcompte = soldeAcompte; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public String getReferencePaiement() { return referencePaiement; }
        public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }
    }

    public Integer getAnneeFiscale() { return anneeFiscale; }
    public void setAnneeFiscale(Integer anneeFiscale) { this.anneeFiscale = anneeFiscale; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public BigDecimal getImpotReference() { return impotReference; }
    public void setImpotReference(BigDecimal impotReference) { this.impotReference = impotReference; }

    public BigDecimal getTotalAcomptesDus() { return totalAcomptesDus; }
    public void setTotalAcomptesDus(BigDecimal totalAcomptesDus) { this.totalAcomptesDus = totalAcomptesDus; }

    public BigDecimal getTotalAcomptesVerses() { return totalAcomptesVerses; }
    public void setTotalAcomptesVerses(BigDecimal totalAcomptesVerses) { this.totalAcomptesVerses = totalAcomptesVerses; }

    public BigDecimal getSoldeRestantAcomptes() { return soldeRestantAcomptes; }
    public void setSoldeRestantAcomptes(BigDecimal soldeRestantAcomptes) { this.soldeRestantAcomptes = soldeRestantAcomptes; }

    public List<AcompteItemDTO> getAcomptes() { return acomptes; }
    public void setAcomptes(List<AcompteItemDTO> acomptes) { this.acomptes = acomptes; }

    public LocalDate getDateLimiteReliquat() { return dateLimiteReliquat; }
    public void setDateLimiteReliquat(LocalDate dateLimiteReliquat) { this.dateLimiteReliquat = dateLimiteReliquat; }

    public BigDecimal getMontantReliquat() { return montantReliquat; }
    public void setMontantReliquat(BigDecimal montantReliquat) { this.montantReliquat = montantReliquat; }

    public String getStatutReliquat() { return statutReliquat; }
    public void setStatutReliquat(String statutReliquat) { this.statutReliquat = statutReliquat; }
}
