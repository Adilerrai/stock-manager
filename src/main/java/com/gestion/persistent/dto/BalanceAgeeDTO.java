package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BalanceAgeeDTO {
    private BigDecimal totalCreances = BigDecimal.ZERO;
    private BigDecimal totalNonEchu = BigDecimal.ZERO;
    private BigDecimal totalMoins30J = BigDecimal.ZERO;
    private BigDecimal total30A60J = BigDecimal.ZERO;
    private BigDecimal total60A90J = BigDecimal.ZERO;
    private BigDecimal totalPlus90J = BigDecimal.ZERO;
    private List<LigneBalanceAgeeDTO> tiers = new ArrayList<>();

    public static class LigneBalanceAgeeDTO {
        private Long tiersId;
        private String tiersNom;
        private String telephone;
        private BigDecimal totalDu = BigDecimal.ZERO;
        private BigDecimal nonEchu = BigDecimal.ZERO;
        private BigDecimal moins30J = BigDecimal.ZERO;
        private BigDecimal de30A60J = BigDecimal.ZERO;
        private BigDecimal de60A90J = BigDecimal.ZERO;
        private BigDecimal plus90J = BigDecimal.ZERO;

        public LigneBalanceAgeeDTO() {}

        public Long getTiersId() { return tiersId; }
        public void setTiersId(Long tiersId) { this.tiersId = tiersId; }

        public String getTiersNom() { return tiersNom; }
        public void setTiersNom(String tiersNom) { this.tiersNom = tiersNom; }

        public String getTelephone() { return telephone; }
        public void setTelephone(String telephone) { this.telephone = telephone; }

        public BigDecimal getTotalDu() { return totalDu; }
        public void setTotalDu(BigDecimal totalDu) { this.totalDu = totalDu; }

        public BigDecimal getNonEchu() { return nonEchu; }
        public void setNonEchu(BigDecimal nonEchu) { this.nonEchu = nonEchu; }

        public BigDecimal getMoins30J() { return moins30J; }
        public void setMoins30J(BigDecimal moins30J) { this.moins30J = moins30J; }

        public BigDecimal getDe30A60J() { return de30A60J; }
        public void setDe30A60J(BigDecimal de30A60J) { this.de30A60J = de30A60J; }

        public BigDecimal getDe60A90J() { return de60A90J; }
        public void setDe60A90J(BigDecimal de60A90J) { this.de60A90J = de60A90J; }

        public BigDecimal getPlus90J() { return plus90J; }
        public void setPlus90J(BigDecimal plus90J) { this.plus90J = plus90J; }
    }

    public BalanceAgeeDTO() {}

    public BigDecimal getTotalCreances() { return totalCreances; }
    public void setTotalCreances(BigDecimal totalCreances) { this.totalCreances = totalCreances; }

    public BigDecimal getTotalNonEchu() { return totalNonEchu; }
    public void setTotalNonEchu(BigDecimal totalNonEchu) { this.totalNonEchu = totalNonEchu; }

    public BigDecimal getTotalMoins30J() { return totalMoins30J; }
    public void setTotalMoins30J(BigDecimal totalMoins30J) { this.totalMoins30J = totalMoins30J; }

    public BigDecimal getTotal30A60J() { return total30A60J; }
    public void setTotal30A60J(BigDecimal total30A60J) { this.total30A60J = total30A60J; }

    public BigDecimal getTotal60A90J() { return total60A90J; }
    public void setTotal60A90J(BigDecimal total60A90J) { this.total60A90J = total60A90J; }

    public BigDecimal getTotalPlus90J() { return totalPlus90J; }
    public void setTotalPlus90J(BigDecimal totalPlus90J) { this.totalPlus90J = totalPlus90J; }

    public List<LigneBalanceAgeeDTO> getTiers() { return tiers; }
    public void setTiers(List<LigneBalanceAgeeDTO> tiers) { this.tiers = tiers; }
}
