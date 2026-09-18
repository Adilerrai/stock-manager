package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CpcOfficielDTO {

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Long tenantId;

    // I. PRODUITS D'EXPLOITATION
    private RubriqueCpcDTO produitsExploitation = new RubriqueCpcDTO("I. PRODUITS D'EXPLOITATION");
    private BigDecimal chiffreAffaires = BigDecimal.ZERO;

    // II. CHARGES D'EXPLOITATION
    private RubriqueCpcDTO chargesExploitation = new RubriqueCpcDTO("II. CHARGES D'EXPLOITATION");

    // III. RÉSULTAT D'EXPLOITATION (I - II)
    private BigDecimal resultatExploitation = BigDecimal.ZERO;

    // IV. PRODUITS FINANCIERS
    private RubriqueCpcDTO produitsFinanciers = new RubriqueCpcDTO("IV. PRODUITS FINANCIERS");

    // V. CHARGES FINANCIÈRES
    private RubriqueCpcDTO chargesFinancieres = new RubriqueCpcDTO("V. CHARGES FINANCIÈRES");

    // VI. RÉSULTAT FINANCIER (IV - V)
    private BigDecimal resultatFinancier = BigDecimal.ZERO;

    // VII. RÉSULTAT COURANT (III + VI)
    private BigDecimal resultatCourant = BigDecimal.ZERO;

    // VIII. PRODUITS NON COURANTS
    private RubriqueCpcDTO produitsNonCourants = new RubriqueCpcDTO("VIII. PRODUITS NON COURANTS");

    // IX. CHARGES NON COURANTES
    private RubriqueCpcDTO chargesNonCourantes = new RubriqueCpcDTO("IX. CHARGES NON COURANTES");

    // X. RÉSULTAT NON COURANT (VIII - IX)
    private BigDecimal resultatNonCourant = BigDecimal.ZERO;

    // XI. RÉSULTAT AVANT IMPÔTS (VII + X)
    private BigDecimal resultatAvantImpots = BigDecimal.ZERO;

    // XII. IMPÔTS SUR LES RÉSULTATS (Rubrique 67)
    private BigDecimal impotSurResultats = BigDecimal.ZERO;

    // XIII. RÉSULTAT NET (XI - XII)
    private BigDecimal resultatNet = BigDecimal.ZERO;

    // TOTAUX RÉCAPITULATIFS
    private BigDecimal totalProduits = BigDecimal.ZERO; // I + IV + VIII
    private BigDecimal totalCharges = BigDecimal.ZERO;  // II + V + IX + XII

    public CpcOfficielDTO() {}

    public static class LigneCpcDTO {
        private String code;
        private String libelle;
        private BigDecimal montant = BigDecimal.ZERO;

        public LigneCpcDTO() {}

        public LigneCpcDTO(String code, String libelle, BigDecimal montant) {
            this.code = code;
            this.libelle = libelle;
            this.montant = montant != null ? montant : BigDecimal.ZERO;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public static class RubriqueCpcDTO {
        private String titre;
        private BigDecimal total = BigDecimal.ZERO;
        private List<LigneCpcDTO> lignes = new ArrayList<>();

        public RubriqueCpcDTO() {}

        public RubriqueCpcDTO(String titre) {
            this.titre = titre;
        }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }

        public List<LigneCpcDTO> getLignes() { return lignes; }
        public void setLignes(List<LigneCpcDTO> lignes) { this.lignes = lignes; }
    }

    // Getters & Setters
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public RubriqueCpcDTO getProduitsExploitation() { return produitsExploitation; }
    public void setProduitsExploitation(RubriqueCpcDTO produitsExploitation) { this.produitsExploitation = produitsExploitation; }

    public BigDecimal getChiffreAffaires() { return chiffreAffaires; }
    public void setChiffreAffaires(BigDecimal chiffreAffaires) { this.chiffreAffaires = chiffreAffaires; }

    public RubriqueCpcDTO getChargesExploitation() { return chargesExploitation; }
    public void setChargesExploitation(RubriqueCpcDTO chargesExploitation) { this.chargesExploitation = chargesExploitation; }

    public BigDecimal getResultatExploitation() { return resultatExploitation; }
    public void setResultatExploitation(BigDecimal resultatExploitation) { this.resultatExploitation = resultatExploitation; }

    public RubriqueCpcDTO getProduitsFinanciers() { return produitsFinanciers; }
    public void setProduitsFinanciers(RubriqueCpcDTO produitsFinanciers) { this.produitsFinanciers = produitsFinanciers; }

    public RubriqueCpcDTO getChargesFinancieres() { return chargesFinancieres; }
    public void setChargesFinancieres(RubriqueCpcDTO chargesFinancieres) { this.chargesFinancieres = chargesFinancieres; }

    public BigDecimal getResultatFinancier() { return resultatFinancier; }
    public void setResultatFinancier(BigDecimal resultatFinancier) { this.resultatFinancier = resultatFinancier; }

    public BigDecimal getResultatCourant() { return resultatCourant; }
    public void setResultatCourant(BigDecimal resultatCourant) { this.resultatCourant = resultatCourant; }

    public RubriqueCpcDTO getProduitsNonCourants() { return produitsNonCourants; }
    public void setProduitsNonCourants(RubriqueCpcDTO produitsNonCourants) { this.produitsNonCourants = produitsNonCourants; }

    public RubriqueCpcDTO getChargesNonCourantes() { return chargesNonCourantes; }
    public void setChargesNonCourantes(RubriqueCpcDTO chargesNonCourantes) { this.chargesNonCourantes = chargesNonCourantes; }

    public BigDecimal getResultatNonCourant() { return resultatNonCourant; }
    public void setResultatNonCourant(BigDecimal resultatNonCourant) { this.resultatNonCourant = resultatNonCourant; }

    public BigDecimal getResultatAvantImpots() { return resultatAvantImpots; }
    public void setResultatAvantImpots(BigDecimal resultatAvantImpots) { this.resultatAvantImpots = resultatAvantImpots; }

    public BigDecimal getImpotSurResultats() { return impotSurResultats; }
    public void setImpotSurResultats(BigDecimal impotSurResultats) { this.impotSurResultats = impotSurResultats; }

    public BigDecimal getResultatNet() { return resultatNet; }
    public void setResultatNet(BigDecimal resultatNet) { this.resultatNet = resultatNet; }

    public BigDecimal getTotalProduits() { return totalProduits; }
    public void setTotalProduits(BigDecimal totalProduits) { this.totalProduits = totalProduits; }

    public BigDecimal getTotalCharges() { return totalCharges; }
    public void setTotalCharges(BigDecimal totalCharges) { this.totalCharges = totalCharges; }
}
