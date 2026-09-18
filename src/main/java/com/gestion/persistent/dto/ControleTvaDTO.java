package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ControleTvaDTO {
    private String periode;
    private BigDecimal tvaCollecteeCommerciale = BigDecimal.ZERO;
    private BigDecimal tvaCollecteeComptable = BigDecimal.ZERO;
    private BigDecimal ecartTvaCollectee = BigDecimal.ZERO;

    private BigDecimal tvaDeductibleCommerciale = BigDecimal.ZERO;
    private BigDecimal tvaDeductibleComptable = BigDecimal.ZERO;
    private BigDecimal ecartTvaDeductible = BigDecimal.ZERO;

    private BigDecimal tvaDeclaree = BigDecimal.ZERO;
    private boolean coherent = true;
    private List<String> alertes = new ArrayList<>();

    public ControleTvaDTO() {}

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public BigDecimal getTvaCollecteeCommerciale() { return tvaCollecteeCommerciale; }
    public void setTvaCollecteeCommerciale(BigDecimal tvaCollecteeCommerciale) { this.tvaCollecteeCommerciale = tvaCollecteeCommerciale; }

    public BigDecimal getTvaCollecteeComptable() { return tvaCollecteeComptable; }
    public void setTvaCollecteeComptable(BigDecimal tvaCollecteeComptable) { this.tvaCollecteeComptable = tvaCollecteeComptable; }

    public BigDecimal getEcartTvaCollectee() { return ecartTvaCollectee; }
    public void setEcartTvaCollectee(BigDecimal ecartTvaCollectee) { this.ecartTvaCollectee = ecartTvaCollectee; }

    public BigDecimal getTvaDeductibleCommerciale() { return tvaDeductibleCommerciale; }
    public void setTvaDeductibleCommerciale(BigDecimal tvaDeductibleCommerciale) { this.tvaDeductibleCommerciale = tvaDeductibleCommerciale; }

    public BigDecimal getTvaDeductibleComptable() { return tvaDeductibleComptable; }
    public void setTvaDeductibleComptable(BigDecimal tvaDeductibleComptable) { this.tvaDeductibleComptable = tvaDeductibleComptable; }

    public BigDecimal getEcartTvaDeductible() { return ecartTvaDeductible; }
    public void setEcartTvaDeductible(BigDecimal ecartTvaDeductible) { this.ecartTvaDeductible = ecartTvaDeductible; }

    public BigDecimal getTvaDeclaree() { return tvaDeclaree; }
    public void setTvaDeclaree(BigDecimal tvaDeclaree) { this.tvaDeclaree = tvaDeclaree; }

    public boolean isCoherent() { return coherent; }
    public void setCoherent(boolean coherent) { this.coherent = coherent; }

    public List<String> getAlertes() { return alertes; }
    public void setAlertes(List<String> alertes) { this.alertes = alertes; }
}
