package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ComparateurOffresDTO {
    private Long demandePrixId;
    private String numeroDdp;
    private String objet;
    private String statut;
    private List<ArticleLigneComparatif> articles = new ArrayList<>();
    private List<FournisseurColonneComparatif> offres = new ArrayList<>();
    private Long meilleureOffreGlobaleId;
    private BigDecimal meilleurMontantTotalHT;
    private BigDecimal economieEstimee;

    public ComparateurOffresDTO() {}

    public static class ArticleLigneComparatif {
        private Long ligneDdpId;
        private Long produitId;
        private String reference;
        private String designation;
        private BigDecimal quantiteDemandee;
        private String unite;
        private Long meilleurFournisseurId;
        private BigDecimal prixUnitaireMinimum;

        public ArticleLigneComparatif() {}

        public Long getLigneDdpId() { return ligneDdpId; }
        public void setLigneDdpId(Long ligneDdpId) { this.ligneDdpId = ligneDdpId; }
        public Long getProduitId() { return produitId; }
        public void setProduitId(Long produitId) { this.produitId = produitId; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }
        public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
        public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }
        public String getUnite() { return unite; }
        public void setUnite(String unite) { this.unite = unite; }
        public Long getMeilleurFournisseurId() { return meilleurFournisseurId; }
        public void setMeilleurFournisseurId(Long meilleurFournisseurId) { this.meilleurFournisseurId = meilleurFournisseurId; }
        public BigDecimal getPrixUnitaireMinimum() { return prixUnitaireMinimum; }
        public void setPrixUnitaireMinimum(BigDecimal prixUnitaireMinimum) { this.prixUnitaireMinimum = prixUnitaireMinimum; }
    }

    public static class FournisseurColonneComparatif {
        private Long offreId;
        private Long fournisseurId;
        private String nomFournisseur;
        private String numeroDevis;
        private Integer delaiJours;
        private BigDecimal fraisPort;
        private String conditionsPaiement;
        private BigDecimal totalHT;
        private String statut;
        private List<PrixArticleCellule> propositions = new ArrayList<>();

        public FournisseurColonneComparatif() {}

        public Long getOffreId() { return offreId; }
        public void setOffreId(Long offreId) { this.offreId = offreId; }
        public Long getFournisseurId() { return fournisseurId; }
        public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }
        public String getNomFournisseur() { return nomFournisseur; }
        public void setNomFournisseur(String nomFournisseur) { this.nomFournisseur = nomFournisseur; }
        public String getNumeroDevis() { return numeroDevis; }
        public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }
        public Integer getDelaiJours() { return delaiJours; }
        public void setDelaiJours(Integer delaiJours) { this.delaiJours = delaiJours; }
        public BigDecimal getFraisPort() { return fraisPort; }
        public void setFraisPort(BigDecimal fraisPort) { this.fraisPort = fraisPort; }
        public String getConditionsPaiement() { return conditionsPaiement; }
        public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }
        public BigDecimal getTotalHT() { return totalHT; }
        public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
        public List<PrixArticleCellule> getPropositions() { return propositions; }
        public void setPropositions(List<PrixArticleCellule> propositions) { this.propositions = propositions; }
    }

    public static class PrixArticleCellule {
        private Long ligneDdpId;
        private BigDecimal prixUnitaireHT;
        private BigDecimal remisePourcentage;
        private BigDecimal quantiteProposee;
        private Boolean disponibleStock;
        private Boolean estMoinsCher;

        public PrixArticleCellule() {}

        public Long getLigneDdpId() { return ligneDdpId; }
        public void setLigneDdpId(Long ligneDdpId) { this.ligneDdpId = ligneDdpId; }
        public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
        public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }
        public BigDecimal getRemisePourcentage() { return remisePourcentage; }
        public void setRemisePourcentage(BigDecimal remisePourcentage) { this.remisePourcentage = remisePourcentage; }
        public BigDecimal getQuantiteProposee() { return quantiteProposee; }
        public void setQuantiteProposee(BigDecimal quantiteProposee) { this.quantiteProposee = quantiteProposee; }
        public Boolean getDisponibleStock() { return disponibleStock; }
        public void setDisponibleStock(Boolean disponibleStock) { this.disponibleStock = disponibleStock; }
        public Boolean getEstMoinsCher() { return estMoinsCher; }
        public void setEstMoinsCher(Boolean estMoinsCher) { this.estMoinsCher = estMoinsCher; }
    }

    public Long getDemandePrixId() { return demandePrixId; }
    public void setDemandePrixId(Long demandePrixId) { this.demandePrixId = demandePrixId; }
    public String getNumeroDdp() { return numeroDdp; }
    public void setNumeroDdp(String numeroDdp) { this.numeroDdp = numeroDdp; }
    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public List<ArticleLigneComparatif> getArticles() { return articles; }
    public void setArticles(List<ArticleLigneComparatif> articles) { this.articles = articles; }
    public List<FournisseurColonneComparatif> getOffres() { return offres; }
    public void setOffres(List<FournisseurColonneComparatif> offres) { this.offres = offres; }
    public Long getMeilleureOffreGlobaleId() { return meilleureOffreGlobaleId; }
    public void setMeilleureOffreGlobaleId(Long meilleureOffreGlobaleId) { this.meilleureOffreGlobaleId = meilleureOffreGlobaleId; }
    public BigDecimal getMeilleurMontantTotalHT() { return meilleurMontantTotalHT; }
    public void setMeilleurMontantTotalHT(BigDecimal meilleurMontantTotalHT) { this.meilleurMontantTotalHT = meilleurMontantTotalHT; }
    public BigDecimal getEconomieEstimee() { return economieEstimee; }
    public void setEconomieEstimee(BigDecimal economieEstimee) { this.economieEstimee = economieEstimee; }
}
