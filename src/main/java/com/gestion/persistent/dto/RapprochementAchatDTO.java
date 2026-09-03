package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutConformiteAchat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RapprochementAchatDTO {
    private Long factureAchatId;
    private String numeroFactureAchat;
    private LocalDateTime dateFacture;

    private Long commandeFournisseurId;
    private String numeroCommandeFournisseur;

    private Long livraisonId;
    private String numeroLivraison;

    private Long fournisseurId;
    private String fournisseurNom;

    // Totaux financiers
    private BigDecimal montantCommandeHT = BigDecimal.ZERO;
    private BigDecimal montantFactureHT = BigDecimal.ZERO;
    private BigDecimal montantFactureTTC = BigDecimal.ZERO;
    private BigDecimal ecartFinancierGlobal = BigDecimal.ZERO;

    // Statut & Décision
    private StatutConformiteAchat statutGlobal = StatutConformiteAchat.CONFORME;
    private String statutGlobalLibelle;
    private Boolean bloquerPaiement = false;
    private String motifBlocage;

    private List<LigneRapprochementDTO> lignes = new ArrayList<>();

    public RapprochementAchatDTO() {}

    public Long getFactureAchatId() { return factureAchatId; }
    public void setFactureAchatId(Long factureAchatId) { this.factureAchatId = factureAchatId; }

    public String getNumeroFactureAchat() { return numeroFactureAchat; }
    public void setNumeroFactureAchat(String numeroFactureAchat) { this.numeroFactureAchat = numeroFactureAchat; }

    public LocalDateTime getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDateTime dateFacture) { this.dateFacture = dateFacture; }

    public Long getCommandeFournisseurId() { return commandeFournisseurId; }
    public void setCommandeFournisseurId(Long commandeFournisseurId) { this.commandeFournisseurId = commandeFournisseurId; }

    public String getNumeroCommandeFournisseur() { return numeroCommandeFournisseur; }
    public void setNumeroCommandeFournisseur(String numeroCommandeFournisseur) { this.numeroCommandeFournisseur = numeroCommandeFournisseur; }

    public Long getLivraisonId() { return livraisonId; }
    public void setLivraisonId(Long livraisonId) { this.livraisonId = livraisonId; }

    public String getNumeroLivraison() { return numeroLivraison; }
    public void setNumeroLivraison(String numeroLivraison) { this.numeroLivraison = numeroLivraison; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public BigDecimal getMontantCommandeHT() { return montantCommandeHT; }
    public void setMontantCommandeHT(BigDecimal montantCommandeHT) { this.montantCommandeHT = montantCommandeHT; }

    public BigDecimal getMontantFactureHT() { return montantFactureHT; }
    public void setMontantFactureHT(BigDecimal montantFactureHT) { this.montantFactureHT = montantFactureHT; }

    public BigDecimal getMontantFactureTTC() { return montantFactureTTC; }
    public void setMontantFactureTTC(BigDecimal montantFactureTTC) { this.montantFactureTTC = montantFactureTTC; }

    public BigDecimal getEcartFinancierGlobal() { return ecartFinancierGlobal; }
    public void setEcartFinancierGlobal(BigDecimal ecartFinancierGlobal) { this.ecartFinancierGlobal = ecartFinancierGlobal; }

    public StatutConformiteAchat getStatutGlobal() { return statutGlobal; }
    public void setStatutGlobal(StatutConformiteAchat statutGlobal) {
        this.statutGlobal = statutGlobal;
        if (statutGlobal != null) this.statutGlobalLibelle = statutGlobal.getLibelle();
    }

    public String getStatutGlobalLibelle() { return statutGlobalLibelle; }
    public void setStatutGlobalLibelle(String statutGlobalLibelle) { this.statutGlobalLibelle = statutGlobalLibelle; }

    public Boolean getBloquerPaiement() { return bloquerPaiement; }
    public void setBloquerPaiement(Boolean bloquerPaiement) { this.bloquerPaiement = bloquerPaiement; }

    public String getMotifBlocage() { return motifBlocage; }
    public void setMotifBlocage(String motifBlocage) { this.motifBlocage = motifBlocage; }

    public List<LigneRapprochementDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneRapprochementDTO> lignes) { this.lignes = lignes; }
}
