package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CpcAnalytiqueDTO {
    private Long centreId;
    private String centreCode;
    private String centreLibelle;
    private String axeLibelle;
    private String typeCentre;
    private LocalDate dateDebut;
    private LocalDate dateFin;

    private BigDecimal totalProduits = BigDecimal.ZERO;
    private BigDecimal totalCharges = BigDecimal.ZERO;
    private BigDecimal resultatNetAnalytique = BigDecimal.ZERO;
    private BigDecimal tauxMarge = BigDecimal.ZERO; // (Produits - Charges) / Produits en %

    private List<PosteAnalytiqueDTO> detailsCharges = new ArrayList<>();
    private List<PosteAnalytiqueDTO> detailsProduits = new ArrayList<>();

    public static class PosteAnalytiqueDTO {
        private String compteNumero;
        private String compteLibelle;
        private BigDecimal montant;

        public PosteAnalytiqueDTO() {}

        public PosteAnalytiqueDTO(String compteNumero, String compteLibelle, BigDecimal montant) {
            this.compteNumero = compteNumero;
            this.compteLibelle = compteLibelle;
            this.montant = montant;
        }

        public String getCompteNumero() { return compteNumero; }
        public void setCompteNumero(String compteNumero) { this.compteNumero = compteNumero; }

        public String getCompteLibelle() { return compteLibelle; }
        public void setCompteLibelle(String compteLibelle) { this.compteLibelle = compteLibelle; }

        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public CpcAnalytiqueDTO() {}

    public Long getCentreId() { return centreId; }
    public void setCentreId(Long centreId) { this.centreId = centreId; }

    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String centreCode) { this.centreCode = centreCode; }

    public String getCentreLibelle() { return centreLibelle; }
    public void setCentreLibelle(String centreLibelle) { this.centreLibelle = centreLibelle; }

    public String getAxeLibelle() { return axeLibelle; }
    public void setAxeLibelle(String axeLibelle) { this.axeLibelle = axeLibelle; }

    public String getTypeCentre() { return typeCentre; }
    public void setTypeCentre(String typeCentre) { this.typeCentre = typeCentre; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public BigDecimal getTotalProduits() { return totalProduits; }
    public void setTotalProduits(BigDecimal totalProduits) { this.totalProduits = totalProduits; }

    public BigDecimal getTotalCharges() { return totalCharges; }
    public void setTotalCharges(BigDecimal totalCharges) { this.totalCharges = totalCharges; }

    public BigDecimal getResultatNetAnalytique() { return resultatNetAnalytique; }
    public void setResultatNetAnalytique(BigDecimal resultatNetAnalytique) { this.resultatNetAnalytique = resultatNetAnalytique; }

    public BigDecimal getTauxMarge() { return tauxMarge; }
    public void setTauxMarge(BigDecimal tauxMarge) { this.tauxMarge = tauxMarge; }

    public List<PosteAnalytiqueDTO> getDetailsCharges() { return detailsCharges; }
    public void setDetailsCharges(List<PosteAnalytiqueDTO> detailsCharges) { this.detailsCharges = detailsCharges; }

    public List<PosteAnalytiqueDTO> getDetailsProduits() { return detailsProduits; }
    public void setDetailsProduits(List<PosteAnalytiqueDTO> detailsProduits) { this.detailsProduits = detailsProduits; }
}
