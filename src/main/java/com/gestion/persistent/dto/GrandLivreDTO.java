package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GrandLivreDTO {
    private String numeroCompte;
    private String libelleCompte;
    private Integer classe;
    private BigDecimal soldeInitialDebit = BigDecimal.ZERO;
    private BigDecimal soldeInitialCredit = BigDecimal.ZERO;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal soldeFinalDebit = BigDecimal.ZERO;
    private BigDecimal soldeFinalCredit = BigDecimal.ZERO;
    private List<LigneGrandLivreItemDTO> mouvements = new ArrayList<>();

    public GrandLivreDTO() {}

    public static class LigneGrandLivreItemDTO {
        private LocalDate date;
        private String journalCode;
        private String numeroPiece;
        private String libelle;
        private BigDecimal debit = BigDecimal.ZERO;
        private BigDecimal credit = BigDecimal.ZERO;
        private BigDecimal soldeProgressif = BigDecimal.ZERO;

        public LigneGrandLivreItemDTO() {}

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public String getJournalCode() { return journalCode; }
        public void setJournalCode(String journalCode) { this.journalCode = journalCode; }

        public String getNumeroPiece() { return numeroPiece; }
        public void setNumeroPiece(String numeroPiece) { this.numeroPiece = numeroPiece; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getDebit() { return debit; }
        public void setDebit(BigDecimal debit) { this.debit = debit; }

        public BigDecimal getCredit() { return credit; }
        public void setCredit(BigDecimal credit) { this.credit = credit; }

        public BigDecimal getSoldeProgressif() { return soldeProgressif; }
        public void setSoldeProgressif(BigDecimal soldeProgressif) { this.soldeProgressif = soldeProgressif; }
    }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelleCompte() { return libelleCompte; }
    public void setLibelleCompte(String libelleCompte) { this.libelleCompte = libelleCompte; }

    public Integer getClasse() { return classe; }
    public void setClasse(Integer classe) { this.classe = classe; }

    public BigDecimal getSoldeInitialDebit() { return soldeInitialDebit; }
    public void setSoldeInitialDebit(BigDecimal soldeInitialDebit) { this.soldeInitialDebit = soldeInitialDebit; }

    public BigDecimal getSoldeInitialCredit() { return soldeInitialCredit; }
    public void setSoldeInitialCredit(BigDecimal soldeInitialCredit) { this.soldeInitialCredit = soldeInitialCredit; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public BigDecimal getSoldeFinalDebit() { return soldeFinalDebit; }
    public void setSoldeFinalDebit(BigDecimal soldeFinalDebit) { this.soldeFinalDebit = soldeFinalDebit; }

    public BigDecimal getSoldeFinalCredit() { return soldeFinalCredit; }
    public void setSoldeFinalCredit(BigDecimal soldeFinalCredit) { this.soldeFinalCredit = soldeFinalCredit; }

    public List<LigneGrandLivreItemDTO> getMouvements() { return mouvements; }
    public void setMouvements(List<LigneGrandLivreItemDTO> mouvements) { this.mouvements = mouvements; }
}
