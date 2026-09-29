package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneSaisieKilometriqueDTO {

    private String numeroCompte; // ex: "61110000", "44110001", etc.
    private String libelleCompte;
    private String libelleLigne;
    private String referenceLigne;
    private BigDecimal debit = BigDecimal.ZERO;
    private BigDecimal credit = BigDecimal.ZERO;

    public LigneSaisieKilometriqueDTO() {}

    public LigneSaisieKilometriqueDTO(String numeroCompte, String libelleCompte, String libelleLigne, BigDecimal debit, BigDecimal credit) {
        this.numeroCompte = numeroCompte;
        this.libelleCompte = libelleCompte;
        this.libelleLigne = libelleLigne;
        this.debit = debit != null ? debit : BigDecimal.ZERO;
        this.credit = credit != null ? credit : BigDecimal.ZERO;
    }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelleCompte() { return libelleCompte; }
    public void setLibelleCompte(String libelleCompte) { this.libelleCompte = libelleCompte; }

    public String getLibelleLigne() { return libelleLigne; }
    public void setLibelleLigne(String libelleLigne) { this.libelleLigne = libelleLigne; }

    public String getReferenceLigne() { return referenceLigne; }
    public void setReferenceLigne(String referenceLigne) { this.referenceLigne = referenceLigne; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
}
