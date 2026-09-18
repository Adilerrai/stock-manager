package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class VentilationAnalytiqueDTO {
    private Long id;
    private Long ligneEcritureId;
    private Long centreAnalytiqueId;
    private String centreCode;
    private String centreLibelle;
    private String axeLibelle;
    private BigDecimal pourcentage;
    private BigDecimal montant;

    public VentilationAnalytiqueDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getLigneEcritureId() { return ligneEcritureId; }
    public void setLigneEcritureId(Long ligneEcritureId) { this.ligneEcritureId = ligneEcritureId; }

    public Long getCentreAnalytiqueId() { return centreAnalytiqueId; }
    public void setCentreAnalytiqueId(Long centreAnalytiqueId) { this.centreAnalytiqueId = centreAnalytiqueId; }

    public String getCentreCode() { return centreCode; }
    public void setCentreCode(String centreCode) { this.centreCode = centreCode; }

    public String getCentreLibelle() { return centreLibelle; }
    public void setCentreLibelle(String centreLibelle) { this.centreLibelle = centreLibelle; }

    public String getAxeLibelle() { return axeLibelle; }
    public void setAxeLibelle(String axeLibelle) { this.axeLibelle = axeLibelle; }

    public BigDecimal getPourcentage() { return pourcentage; }
    public void setPourcentage(BigDecimal pourcentage) { this.pourcentage = pourcentage; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
}
