package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class PortefeuilleStatsDTO {
    private BigDecimal totalEnPortefeuille = BigDecimal.ZERO;
    private BigDecimal totalRemisBanque = BigDecimal.ZERO;
    private BigDecimal totalEncaisse = BigDecimal.ZERO;
    private BigDecimal totalRejeteImpaye = BigDecimal.ZERO;
    private Long nbChequesEnPortefeuille = 0L;
    private Long nbChequesAEcheance = 0L;

    public PortefeuilleStatsDTO() {}

    public BigDecimal getTotalEnPortefeuille() { return totalEnPortefeuille; }
    public void setTotalEnPortefeuille(BigDecimal totalEnPortefeuille) { this.totalEnPortefeuille = totalEnPortefeuille; }

    public BigDecimal getTotalRemisBanque() { return totalRemisBanque; }
    public void setTotalRemisBanque(BigDecimal totalRemisBanque) { this.totalRemisBanque = totalRemisBanque; }

    public BigDecimal getTotalEncaisse() { return totalEncaisse; }
    public void setTotalEncaisse(BigDecimal totalEncaisse) { this.totalEncaisse = totalEncaisse; }

    public BigDecimal getTotalRejeteImpaye() { return totalRejeteImpaye; }
    public void setTotalRejeteImpaye(BigDecimal totalRejeteImpaye) { this.totalRejeteImpaye = totalRejeteImpaye; }

    public Long getNbChequesEnPortefeuille() { return nbChequesEnPortefeuille; }
    public void setNbChequesEnPortefeuille(Long nbChequesEnPortefeuille) { this.nbChequesEnPortefeuille = nbChequesEnPortefeuille; }

    public Long getNbChequesAEcheance() { return nbChequesAEcheance; }
    public void setNbChequesAEcheance(Long nbChequesAEcheance) { this.nbChequesAEcheance = nbChequesAEcheance; }
}
