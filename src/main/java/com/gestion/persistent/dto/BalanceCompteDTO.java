package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class BalanceCompteDTO {
    private String numeroCompte;
    private String libelleCompte;
    private Integer classe;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private BigDecimal soldeDebiteur = BigDecimal.ZERO;
    private BigDecimal soldeCrediteur = BigDecimal.ZERO;

    public BalanceCompteDTO() {}

    public BalanceCompteDTO(String numeroCompte, String libelleCompte, Integer classe,
                            BigDecimal totalDebit, BigDecimal totalCredit) {
        this.numeroCompte = numeroCompte;
        this.libelleCompte = libelleCompte;
        this.classe = classe;
        this.totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        this.totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;

        BigDecimal diff = this.totalDebit.subtract(this.totalCredit);
        if (diff.compareTo(BigDecimal.ZERO) >= 0) {
            this.soldeDebiteur = diff;
            this.soldeCrediteur = BigDecimal.ZERO;
        } else {
            this.soldeDebiteur = BigDecimal.ZERO;
            this.soldeCrediteur = diff.abs();
        }
    }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelleCompte() { return libelleCompte; }
    public void setLibelleCompte(String libelleCompte) { this.libelleCompte = libelleCompte; }

    public Integer getClasse() { return classe; }
    public void setClasse(Integer classe) { this.classe = classe; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public BigDecimal getSoldeDebiteur() { return soldeDebiteur; }
    public void setSoldeDebiteur(BigDecimal soldeDebiteur) { this.soldeDebiteur = soldeDebiteur; }

    public BigDecimal getSoldeCrediteur() { return soldeCrediteur; }
    public void setSoldeCrediteur(BigDecimal soldeCrediteur) { this.soldeCrediteur = soldeCrediteur; }
}
