package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class LigneDemandeAchatDTO {
    private Long id;
    private Long demandeAchatId;
    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantiteDemandee = BigDecimal.ONE;
    private String unite = "U";
    private String remarques;

    public LigneDemandeAchatDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandeAchatId() { return demandeAchatId; }
    public void setDemandeAchatId(Long demandeAchatId) { this.demandeAchatId = demandeAchatId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantiteDemandee() { return quantiteDemandee; }
    public void setQuantiteDemandee(BigDecimal quantiteDemandee) { this.quantiteDemandee = quantiteDemandee; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public String getRemarques() { return remarques; }
    public void setRemarques(String remarques) { this.remarques = remarques; }
}
