package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MargeDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;

    // Cascade TADBEER stricte
    private BigDecimal chiffreAffairesHT = BigDecimal.ZERO;      // 1. CA Brut HT
    private BigDecimal totalRemises = BigDecimal.ZERO;           // 2. Remises accordées
    private BigDecimal totalRetoursAvoirs = BigDecimal.ZERO;     // 3. Avoirs clients
    private BigDecimal chiffreAffairesNetHT = BigDecimal.ZERO;   // 4. CA Net HT = CA Brut - Remises - Avoirs
    private BigDecimal coutMarchandisesHT = BigDecimal.ZERO;     // 5. Coût des marchandises vendues (PMP)
    private BigDecimal margeCommerciale = BigDecimal.ZERO;       // 6. Marge Commerciale = CA Net - Coût
    private BigDecimal tauxMarge = BigDecimal.ZERO;              // 7. Taux de marge (%) = (Marge / CA Net) * 100

    // Rétrocompatibilité
    private BigDecimal margeBrute = BigDecimal.ZERO;
    private BigDecimal margeNetteCommerciale = BigDecimal.ZERO;

    private List<LigneMargeDTO> margesParProduit = new ArrayList<>();
    private List<LigneMargeDTO> margesParCategorie = new ArrayList<>();
    private List<LigneMargeDTO> margesParClient = new ArrayList<>();

    public MargeDTO() {}

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getChiffreAffairesHT() { return chiffreAffairesHT; }
    public void setChiffreAffairesHT(BigDecimal chiffreAffairesHT) { this.chiffreAffairesHT = chiffreAffairesHT; }

    public BigDecimal getCoutMarchandisesHT() { return coutMarchandisesHT; }
    public void setCoutMarchandisesHT(BigDecimal coutMarchandisesHT) { this.coutMarchandisesHT = coutMarchandisesHT; }

    public BigDecimal getTotalRemises() { return totalRemises; }
    public void setTotalRemises(BigDecimal totalRemises) { this.totalRemises = totalRemises; }

    public BigDecimal getTotalRetoursAvoirs() { return totalRetoursAvoirs; }
    public void setTotalRetoursAvoirs(BigDecimal totalRetoursAvoirs) { this.totalRetoursAvoirs = totalRetoursAvoirs; }

    public BigDecimal getChiffreAffairesNetHT() { return chiffreAffairesNetHT; }
    public void setChiffreAffairesNetHT(BigDecimal chiffreAffairesNetHT) { this.chiffreAffairesNetHT = chiffreAffairesNetHT; }

    public BigDecimal getMargeCommerciale() { return margeCommerciale; }
    public void setMargeCommerciale(BigDecimal margeCommerciale) { this.margeCommerciale = margeCommerciale; }

    public BigDecimal getMargeBrute() { return margeBrute; }
    public void setMargeBrute(BigDecimal margeBrute) { this.margeBrute = margeBrute; }

    public BigDecimal getMargeNetteCommerciale() { return margeNetteCommerciale; }
    public void setMargeNetteCommerciale(BigDecimal margeNetteCommerciale) { this.margeNetteCommerciale = margeNetteCommerciale; }

    public BigDecimal getTauxMarge() { return tauxMarge; }
    public void setTauxMarge(BigDecimal tauxMarge) { this.tauxMarge = tauxMarge; }

    public List<LigneMargeDTO> getMargesParProduit() { return margesParProduit; }
    public void setMargesParProduit(List<LigneMargeDTO> margesParProduit) { this.margesParProduit = margesParProduit; }

    public List<LigneMargeDTO> getMargesParCategorie() { return margesParCategorie; }
    public void setMargesParCategorie(List<LigneMargeDTO> margesParCategorie) { this.margesParCategorie = margesParCategorie; }

    public List<LigneMargeDTO> getMargesParClient() { return margesParClient; }
    public void setMargesParClient(List<LigneMargeDTO> margesParClient) { this.margesParClient = margesParClient; }

    public static class LigneMargeDTO {
        private Long id;
        private String reference;
        private String nom;
        private BigDecimal quantiteVendue = BigDecimal.ZERO;
        private BigDecimal chiffreAffairesHT = BigDecimal.ZERO; // CA Brut
        private BigDecimal coutAchatHT = BigDecimal.ZERO;       // Coût d'achat / PMP
        private BigDecimal remise = BigDecimal.ZERO;
        private BigDecimal avoirsHT = BigDecimal.ZERO;
        private BigDecimal caNetHT = BigDecimal.ZERO;
        private BigDecimal marge = BigDecimal.ZERO;            // Marge commerciale
        private BigDecimal tauxMarge = BigDecimal.ZERO;        // %
        private BigDecimal prixVenteUnitaireMoyen = BigDecimal.ZERO;
        private BigDecimal coutUnitaireMoyen = BigDecimal.ZERO;
        private boolean isSousPmp = false;
        private boolean isMargeNegative = false;
        private boolean isFaibleMarge = false;

        public LigneMargeDTO() {}

        public LigneMargeDTO(Long id, String reference, String nom, BigDecimal quantiteVendue,
                             BigDecimal chiffreAffairesHT, BigDecimal coutAchatHT,
                             BigDecimal remise, BigDecimal marge, BigDecimal tauxMarge) {
            this.id = id;
            this.reference = reference;
            this.nom = nom;
            this.quantiteVendue = quantiteVendue != null ? quantiteVendue : BigDecimal.ZERO;
            this.chiffreAffairesHT = chiffreAffairesHT != null ? chiffreAffairesHT : BigDecimal.ZERO;
            this.coutAchatHT = coutAchatHT != null ? coutAchatHT : BigDecimal.ZERO;
            this.remise = remise != null ? remise : BigDecimal.ZERO;
            this.marge = marge != null ? marge : BigDecimal.ZERO;
            this.tauxMarge = tauxMarge != null ? tauxMarge : BigDecimal.ZERO;
            this.caNetHT = this.chiffreAffairesHT.subtract(this.remise);
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public BigDecimal getQuantiteVendue() { return quantiteVendue; }
        public void setQuantiteVendue(BigDecimal quantiteVendue) { this.quantiteVendue = quantiteVendue; }

        public BigDecimal getChiffreAffairesHT() { return chiffreAffairesHT; }
        public void setChiffreAffairesHT(BigDecimal chiffreAffairesHT) { this.chiffreAffairesHT = chiffreAffairesHT; }

        public BigDecimal getCoutAchatHT() { return coutAchatHT; }
        public void setCoutAchatHT(BigDecimal coutAchatHT) { this.coutAchatHT = coutAchatHT; }

        public BigDecimal getRemise() { return remise; }
        public void setRemise(BigDecimal remise) { this.remise = remise; }

        public BigDecimal getAvoirsHT() { return avoirsHT; }
        public void setAvoirsHT(BigDecimal avoirsHT) { this.avoirsHT = avoirsHT; }

        public BigDecimal getCaNetHT() { return caNetHT; }
        public void setCaNetHT(BigDecimal caNetHT) { this.caNetHT = caNetHT; }

        public BigDecimal getMarge() { return marge; }
        public void setMarge(BigDecimal marge) { this.marge = marge; }

        public BigDecimal getTauxMarge() { return tauxMarge; }
        public void setTauxMarge(BigDecimal tauxMarge) { this.tauxMarge = tauxMarge; }

        public BigDecimal getPrixVenteUnitaireMoyen() { return prixVenteUnitaireMoyen; }
        public void setPrixVenteUnitaireMoyen(BigDecimal prixVenteUnitaireMoyen) { this.prixVenteUnitaireMoyen = prixVenteUnitaireMoyen; }

        public BigDecimal getCoutUnitaireMoyen() { return coutUnitaireMoyen; }
        public void setCoutUnitaireMoyen(BigDecimal coutUnitaireMoyen) { this.coutUnitaireMoyen = coutUnitaireMoyen; }

        public boolean isSousPmp() { return isSousPmp; }
        public void setSousPmp(boolean sousPmp) { isSousPmp = sousPmp; }

        public boolean isMargeNegative() { return isMargeNegative; }
        public void setMargeNegative(boolean margeNegative) { isMargeNegative = margeNegative; }

        public boolean isFaibleMarge() { return isFaibleMarge; }
        public void setFaibleMarge(boolean faibleMarge) { isFaibleMarge = faibleMarge; }
    }
}
