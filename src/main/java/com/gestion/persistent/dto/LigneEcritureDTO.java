package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneEcritureDTO {
    private Long id;
    private Long compteId;
    private String numeroCompte;
    private String libelleCompte;
    private BigDecimal debit;
    private BigDecimal credit;
    private String libelleLigne;
    private String referenceLigne;
    private String lettrage;

    public LigneEcritureDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompteId() { return compteId; }
    public void setCompteId(Long compteId) { this.compteId = compteId; }

    public String getNumeroCompte() { return numeroCompte; }
    public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

    public String getLibelleCompte() { return libelleCompte; }
    public void setLibelleCompte(String libelleCompte) { this.libelleCompte = libelleCompte; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public String getLibelleLigne() { return libelleLigne; }
    public void setLibelleLigne(String libelleLigne) { this.libelleLigne = libelleLigne; }

    public String getReferenceLigne() { return referenceLigne; }
    public void setReferenceLigne(String referenceLigne) { this.referenceLigne = referenceLigne; }

    public String getLettrage() { return lettrage; }
    public void setLettrage(String lettrage) { this.lettrage = lettrage; }
}
