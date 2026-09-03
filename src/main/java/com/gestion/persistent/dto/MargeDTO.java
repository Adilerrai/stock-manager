package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MargeDTO {
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private BigDecimal chiffreAffairesHT = BigDecimal.ZERO;
    private BigDecimal coutMarchandisesHT = BigDecimal.ZERO;
    private BigDecimal totalRemises = BigDecimal.ZERO;
    private BigDecimal totalRetoursAvoirs = BigDecimal.ZERO;
    private BigDecimal margeBrute = BigDecimal.ZERO;
    private BigDecimal margeNetteCommerciale = BigDecimal.ZERO;
    private BigDecimal tauxMarge = BigDecimal.ZERO; // En %

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
        private BigDecimal quantiteVendue;
        private BigDecimal chiffreAffairesHT;
        private BigDecimal coutAchatHT;
        private BigDecimal remise;
        private BigDecimal marge;
        private BigDecimal tauxMarge;

        public LigneMargeDTO() {}

        public LigneMargeDTO(Long id, String reference, String nom, BigDecimal quantiteVendue,
                             BigDecimal chiffreAffairesHT, BigDecimal coutAchatHT,
                             BigDecimal remise, BigDecimal marge, BigDecimal tauxMarge) {
            this.id = id;
            this.reference = reference;
            this.nom = nom;
            this.quantiteVendue = quantiteVendue;
            this.chiffreAffairesHT = chiffreAffairesHT;
            this.coutAchatHT = coutAchatHT;
            this.remise = remise;
            this.marge = marge;
            this.tauxMarge = tauxMarge;
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

        public BigDecimal getMarge() { return marge; }
        public void setMarge(BigDecimal marge) { this.marge = marge; }

        public BigDecimal getTauxMarge() { return tauxMarge; }
        public void setTauxMarge(BigDecimal tauxMarge) { this.tauxMarge = tauxMarge; }
    }
}
