package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReleveFournisseurDTO {
    private Long fournisseurId;
    private String fournisseurNom;
    private String telephone;
    private String email;
    private String ice;
    private String numeroRegistreCommerce;
    private String numeroIdentificationFiscale;
    private String patente;
    private String ribBancaire;
    private String banqueNom;
    private Integer delaiPaiementJours;
    private BigDecimal soldeActuel;
    private BigDecimal totalAchats;
    private BigDecimal totalReglements;
    private List<LigneReleveDTO> operations = new ArrayList<>();

    public static class LigneReleveDTO {
        private LocalDate date;
        private String typeOperation; // FACTURE_ACHAT, REGLEMENT
        private String reference;
        private String libelle;
        private BigDecimal debit;  // Règlement effectué (diminue la dette fournisseur)
        private BigDecimal credit; // Facture reçue (augmente la dette fournisseur)
        private BigDecimal soldeProgressif;

        public LigneReleveDTO() {}

        public LigneReleveDTO(LocalDate date, String typeOperation, String reference, String libelle,
                              BigDecimal debit, BigDecimal credit, BigDecimal soldeProgressif) {
            this.date = date;
            this.typeOperation = typeOperation;
            this.reference = reference;
            this.libelle = libelle;
            this.debit = debit != null ? debit : BigDecimal.ZERO;
            this.credit = credit != null ? credit : BigDecimal.ZERO;
            this.soldeProgressif = soldeProgressif;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getTypeOperation() { return typeOperation; }
        public void setTypeOperation(String typeOperation) { this.typeOperation = typeOperation; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getDebit() { return debit; }
        public void setDebit(BigDecimal debit) { this.debit = debit; }

        public BigDecimal getCredit() { return credit; }
        public void setCredit(BigDecimal credit) { this.credit = credit; }

        public BigDecimal getSoldeProgressif() { return soldeProgressif; }
        public void setSoldeProgressif(BigDecimal soldeProgressif) { this.soldeProgressif = soldeProgressif; }
    }

    public ReleveFournisseurDTO() {}

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getNumeroRegistreCommerce() { return numeroRegistreCommerce; }
    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) { this.numeroRegistreCommerce = numeroRegistreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getRibBancaire() { return ribBancaire; }
    public void setRibBancaire(String ribBancaire) { this.ribBancaire = ribBancaire; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }

    public Integer getDelaiPaiementJours() { return delaiPaiementJours; }
    public void setDelaiPaiementJours(Integer delaiPaiementJours) { this.delaiPaiementJours = delaiPaiementJours; }

    public BigDecimal getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(BigDecimal soldeActuel) { this.soldeActuel = soldeActuel; }

    public BigDecimal getTotalAchats() { return totalAchats; }
    public void setTotalAchats(BigDecimal totalAchats) { this.totalAchats = totalAchats; }

    public BigDecimal getTotalReglements() { return totalReglements; }
    public void setTotalReglements(BigDecimal totalReglements) { this.totalReglements = totalReglements; }

    public List<LigneReleveDTO> getOperations() { return operations; }
    public void setOperations(List<LigneReleveDTO> operations) { this.operations = operations; }
}
