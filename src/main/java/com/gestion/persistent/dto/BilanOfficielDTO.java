package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BilanOfficielDTO {

    private LocalDate dateArrete;
    private Long tenantId;

    // Masses ACTIF
    private RubriqueActifDTO actifImmobilise = new RubriqueActifDTO("I. ACTIF IMMOBILISÉ");
    private RubriqueActifDTO actifCirculant = new RubriqueActifDTO("II. ACTIF CIRCULANT (HORS TRÉSORERIE)");
    private RubriqueActifDTO tresorerieActif = new RubriqueActifDTO("III. TRÉSORERIE - ACTIF");

    private BigDecimal totalActifBrut = BigDecimal.ZERO;
    private BigDecimal totalActifAmortissements = BigDecimal.ZERO;
    private BigDecimal totalActifNet = BigDecimal.ZERO;

    // Masses PASSIF
    private RubriquePassifDTO financementPermanent = new RubriquePassifDTO("I. FINANCEMENT PERMANENT");
    private RubriquePassifDTO passifCirculant = new RubriquePassifDTO("II. PASSIF CIRCULANT (HORS TRÉSORERIE)");
    private RubriquePassifDTO tresoreriePassif = new RubriquePassifDTO("III. TRÉSORERIE - PASSIF");

    private BigDecimal totalPassif = BigDecimal.ZERO;

    // Équilibre
    private BigDecimal resultatNetExercice = BigDecimal.ZERO;
    private BigDecimal ecartEquilibre = BigDecimal.ZERO;
    private boolean equilibre = true;

    public BilanOfficielDTO() {}

    public static class LigneBilanActifDTO {
        private String code;
        private String libelle;
        private BigDecimal brut = BigDecimal.ZERO;
        private BigDecimal amortissementsProvisions = BigDecimal.ZERO;
        private BigDecimal net = BigDecimal.ZERO;

        public LigneBilanActifDTO() {}

        public LigneBilanActifDTO(String code, String libelle, BigDecimal brut, BigDecimal amortissementsProvisions, BigDecimal net) {
            this.code = code;
            this.libelle = libelle;
            this.brut = brut != null ? brut : BigDecimal.ZERO;
            this.amortissementsProvisions = amortissementsProvisions != null ? amortissementsProvisions : BigDecimal.ZERO;
            this.net = net != null ? net : BigDecimal.ZERO;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }
        public BigDecimal getBrut() { return brut; }
        public void setBrut(BigDecimal brut) { this.brut = brut; }
        public BigDecimal getAmortissementsProvisions() { return amortissementsProvisions; }
        public void setAmortissementsProvisions(BigDecimal amortissementsProvisions) { this.amortissementsProvisions = amortissementsProvisions; }
        public BigDecimal getNet() { return net; }
        public void setNet(BigDecimal net) { this.net = net; }
    }

    public static class LigneBilanPassifDTO {
        private String code;
        private String libelle;
        private BigDecimal montant = BigDecimal.ZERO;

        public LigneBilanPassifDTO() {}

        public LigneBilanPassifDTO(String code, String libelle, BigDecimal montant) {
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

    public static class RubriqueActifDTO {
        private String titre;
        private BigDecimal totalBrut = BigDecimal.ZERO;
        private BigDecimal totalAmortissements = BigDecimal.ZERO;
        private BigDecimal totalNet = BigDecimal.ZERO;
        private List<LigneBilanActifDTO> lignes = new ArrayList<>();

        public RubriqueActifDTO() {}
        public RubriqueActifDTO(String titre) { this.titre = titre; }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public BigDecimal getTotalBrut() { return totalBrut; }
        public void setTotalBrut(BigDecimal totalBrut) { this.totalBrut = totalBrut; }
        public BigDecimal getTotalAmortissements() { return totalAmortissements; }
        public void setTotalAmortissements(BigDecimal totalAmortissements) { this.totalAmortissements = totalAmortissements; }
        public BigDecimal getTotalNet() { return totalNet; }
        public void setTotalNet(BigDecimal totalNet) { this.totalNet = totalNet; }
        public List<LigneBilanActifDTO> getLignes() { return lignes; }
        public void setLignes(List<LigneBilanActifDTO> lignes) { this.lignes = lignes; }
    }

    public static class RubriquePassifDTO {
        private String titre;
        private BigDecimal total = BigDecimal.ZERO;
        private List<LigneBilanPassifDTO> lignes = new ArrayList<>();

        public RubriquePassifDTO() {}
        public RubriquePassifDTO(String titre) { this.titre = titre; }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        public BigDecimal getTotal() { return total; }
        public void setTotal(BigDecimal total) { this.total = total; }
        public List<LigneBilanPassifDTO> getLignes() { return lignes; }
        public void setLignes(List<LigneBilanPassifDTO> lignes) { this.lignes = lignes; }
    }

    public LocalDate getDateArrete() { return dateArrete; }
    public void setDateArrete(LocalDate dateArrete) { this.dateArrete = dateArrete; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public RubriqueActifDTO getActifImmobilise() { return actifImmobilise; }
    public void setActifImmobilise(RubriqueActifDTO actifImmobilise) { this.actifImmobilise = actifImmobilise; }
    public RubriqueActifDTO getActifCirculant() { return actifCirculant; }
    public void setActifCirculant(RubriqueActifDTO actifCirculant) { this.actifCirculant = actifCirculant; }
    public RubriqueActifDTO getTresorerieActif() { return tresorerieActif; }
    public void setTresorerieActif(RubriqueActifDTO tresorerieActif) { this.tresorerieActif = tresorerieActif; }
    public BigDecimal getTotalActifBrut() { return totalActifBrut; }
    public void setTotalActifBrut(BigDecimal totalActifBrut) { this.totalActifBrut = totalActifBrut; }
    public BigDecimal getTotalActifAmortissements() { return totalActifAmortissements; }
    public void setTotalActifAmortissements(BigDecimal totalActifAmortissements) { this.totalActifAmortissements = totalActifAmortissements; }
    public BigDecimal getTotalActifNet() { return totalActifNet; }
    public void setTotalActifNet(BigDecimal totalActifNet) { this.totalActifNet = totalActifNet; }
    public RubriquePassifDTO getFinancementPermanent() { return financementPermanent; }
    public void setFinancementPermanent(RubriquePassifDTO financementPermanent) { this.financementPermanent = financementPermanent; }
    public RubriquePassifDTO getPassifCirculant() { return passifCirculant; }
    public void setPassifCirculant(RubriquePassifDTO passifCirculant) { this.passifCirculant = passifCirculant; }
    public RubriquePassifDTO getTresoreriePassif() { return tresoreriePassif; }
    public void setTresoreriePassif(RubriquePassifDTO tresoreriePassif) { this.tresoreriePassif = tresoreriePassif; }
    public BigDecimal getTotalPassif() { return totalPassif; }
    public void setTotalPassif(BigDecimal totalPassif) { this.totalPassif = totalPassif; }
    public BigDecimal getResultatNetExercice() { return resultatNetExercice; }
    public void setResultatNetExercice(BigDecimal resultatNetExercice) { this.resultatNetExercice = resultatNetExercice; }
    public BigDecimal getEcartEquilibre() { return ecartEquilibre; }
    public void setEcartEquilibre(BigDecimal ecartEquilibre) { this.ecartEquilibre = ecartEquilibre; }
    public boolean isEquilibre() { return equilibre; }
    public void setEquilibre(boolean equilibre) { this.equilibre = equilibre; }
}
