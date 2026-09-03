package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.List;

public class SyntheseTresorerieDTO {
    private BigDecimal totalCaisses = BigDecimal.ZERO;
    private BigDecimal totalBanques = BigDecimal.ZERO;
    private BigDecimal tresorerieDisponibleGlobale = BigDecimal.ZERO;

    private List<CompteFinancierDTO> caisses;
    private List<CompteFinancierDTO> comptesBancaires;

    public SyntheseTresorerieDTO() {}

    public BigDecimal getTotalCaisses() { return totalCaisses; }
    public void setTotalCaisses(BigDecimal totalCaisses) { this.totalCaisses = totalCaisses; }

    public BigDecimal getTotalBanques() { return totalBanques; }
    public void setTotalBanques(BigDecimal totalBanques) { this.totalBanques = totalBanques; }

    public BigDecimal getTresorerieDisponibleGlobale() { return tresorerieDisponibleGlobale; }
    public void setTresorerieDisponibleGlobale(BigDecimal tresorerieDisponibleGlobale) { this.tresorerieDisponibleGlobale = tresorerieDisponibleGlobale; }

    public List<CompteFinancierDTO> getCaisses() { return caisses; }
    public void setCaisses(List<CompteFinancierDTO> caisses) { this.caisses = caisses; }

    public List<CompteFinancierDTO> getComptesBancaires() { return comptesBancaires; }
    public void setComptesBancaires(List<CompteFinancierDTO> comptesBancaires) { this.comptesBancaires = comptesBancaires; }
}
