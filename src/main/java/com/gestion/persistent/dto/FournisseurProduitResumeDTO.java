package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FournisseurProduitResumeDTO {

    private Long produitId;
    private String reference;
    private String designation;
    private String categorie;
    private Integer quantiteTotaleCommandee = 0;
    private Integer quantiteTotaleLivree = 0;
    private BigDecimal dernierPrixAchat = BigDecimal.ZERO;
    private BigDecimal prixMoyen = BigDecimal.ZERO;
    private BigDecimal montantTotalHT = BigDecimal.ZERO;
    private BigDecimal montantTotalTTC = BigDecimal.ZERO;
    private Integer nombreCommandes = 0;
    private LocalDateTime dateDerniereCommande;
    private List<CommandeReferenceResumeDTO> commandes = new ArrayList<>();

    public FournisseurProduitResumeDTO() {}

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public Integer getQuantiteTotaleCommandee() { return quantiteTotaleCommandee; }
    public void setQuantiteTotaleCommandee(Integer quantiteTotaleCommandee) { this.quantiteTotaleCommandee = quantiteTotaleCommandee; }

    public Integer getQuantiteTotaleLivree() { return quantiteTotaleLivree; }
    public void setQuantiteTotaleLivree(Integer quantiteTotaleLivree) { this.quantiteTotaleLivree = quantiteTotaleLivree; }

    public BigDecimal getDernierPrixAchat() { return dernierPrixAchat; }
    public void setDernierPrixAchat(BigDecimal dernierPrixAchat) { this.dernierPrixAchat = dernierPrixAchat; }

    public BigDecimal getPrixMoyen() { return prixMoyen; }
    public void setPrixMoyen(BigDecimal prixMoyen) { this.prixMoyen = prixMoyen; }

    public BigDecimal getMontantTotalHT() { return montantTotalHT; }
    public void setMontantTotalHT(BigDecimal montantTotalHT) { this.montantTotalHT = montantTotalHT; }

    public BigDecimal getMontantTotalTTC() { return montantTotalTTC; }
    public void setMontantTotalTTC(BigDecimal montantTotalTTC) { this.montantTotalTTC = montantTotalTTC; }

    public Integer getNombreCommandes() { return nombreCommandes; }
    public void setNombreCommandes(Integer nombreCommandes) { this.nombreCommandes = nombreCommandes; }

    public LocalDateTime getDateDerniereCommande() { return dateDerniereCommande; }
    public void setDateDerniereCommande(LocalDateTime dateDerniereCommande) { this.dateDerniereCommande = dateDerniereCommande; }

    public List<CommandeReferenceResumeDTO> getCommandes() { return commandes; }
    public void setCommandes(List<CommandeReferenceResumeDTO> commandes) { this.commandes = commandes; }

    public static class CommandeReferenceResumeDTO {
        private Long commandeId;
        private String numeroCommande;
        private LocalDateTime dateCommande;
        private String statut;
        private Integer quantiteCommandee;
        private Integer quantiteLivree;
        private BigDecimal prixUnitaireHT;
        private BigDecimal montantHT;

        public CommandeReferenceResumeDTO() {}

        public Long getCommandeId() { return commandeId; }
        public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

        public String getNumeroCommande() { return numeroCommande; }
        public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

        public LocalDateTime getDateCommande() { return dateCommande; }
        public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public Integer getQuantiteCommandee() { return quantiteCommandee; }
        public void setQuantiteCommandee(Integer quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

        public Integer getQuantiteLivree() { return quantiteLivree; }
        public void setQuantiteLivree(Integer quantiteLivree) { this.quantiteLivree = quantiteLivree; }

        public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
        public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

        public BigDecimal getMontantHT() { return montantHT; }
        public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }
    }
}
