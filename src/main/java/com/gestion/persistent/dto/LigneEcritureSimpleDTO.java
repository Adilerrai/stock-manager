package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LigneEcritureSimpleDTO {
    private Long id;
    private Long ecritureId;
    private String numeroPiece;
    private LocalDate dateEcriture;
    private String numeroCompte;
    private String libelleLigne;
    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;
    private String referenceLigne;

    public LigneEcritureSimpleDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEcritureId() {
        return ecritureId;
    }

    public void setEcritureId(Long ecritureId) {
        this.ecritureId = ecritureId;
    }

    public String getNumeroPiece() {
        return numeroPiece;
    }

    public void setNumeroPiece(String numeroPiece) {
        this.numeroPiece = numeroPiece;
    }

    public LocalDate getDateEcriture() {
        return dateEcriture;
    }

    public void setDateEcriture(LocalDate dateEcriture) {
        this.dateEcriture = dateEcriture;
    }

    public String getNumeroCompte() {
        return numeroCompte;
    }

    public void setNumeroCompte(String numeroCompte) {
        this.numeroCompte = numeroCompte;
    }

    public String getLibelleLigne() {
        return libelleLigne;
    }

    public void setLibelleLigne(String libelleLigne) {
        this.libelleLigne = libelleLigne;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public String getReferenceLigne() {
        return referenceLigne;
    }

    public void setReferenceLigne(String referenceLigne) {
        this.referenceLigne = referenceLigne;
    }
}
