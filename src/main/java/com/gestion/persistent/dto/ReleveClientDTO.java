package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReleveClientDTO {
    private Long clientId;
    private String clientNom;
    private String telephone;
    private String email;
    private String ice;
    private BigDecimal creditAutorise;
    private BigDecimal soldeActuel;
    private BigDecimal totalFactures;
    private BigDecimal totalPaiements;
    private BigDecimal totalAvoirs;
    private List<LigneReleveDTO> operations = new ArrayList<>();

    public static class LigneReleveDTO {
        private LocalDate date;
        private String typeOperation; // FACTURE, PAIEMENT, AVOIR
        private String reference;
        private String libelle;
        private BigDecimal debit;  // Montant facturé (augmente la créance)
        private BigDecimal credit; // Montant payé ou avoir (diminue la créance)
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

    public ReleveClientDTO() {}

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public BigDecimal getCreditAutorise() { return creditAutorise; }
    public void setCreditAutorise(BigDecimal creditAutorise) { this.creditAutorise = creditAutorise; }

    public BigDecimal getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(BigDecimal soldeActuel) { this.soldeActuel = soldeActuel; }

    public BigDecimal getTotalFactures() { return totalFactures; }
    public void setTotalFactures(BigDecimal totalFactures) { this.totalFactures = totalFactures; }

    public BigDecimal getTotalPaiements() { return totalPaiements; }
    public void setTotalPaiements(BigDecimal totalPaiements) { this.totalPaiements = totalPaiements; }

    public BigDecimal getTotalAvoirs() { return totalAvoirs; }
    public void setTotalAvoirs(BigDecimal totalAvoirs) { this.totalAvoirs = totalAvoirs; }

    public List<LigneReleveDTO> getOperations() { return operations; }
    public void setOperations(List<LigneReleveDTO> operations) { this.operations = operations; }
}
