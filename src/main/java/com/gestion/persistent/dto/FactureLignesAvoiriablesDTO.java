package com.gestion.persistent.dto;

import com.gestion.persistent.enums.TypeAvoir;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class FactureLignesAvoiriablesDTO {
    private Long factureId;
    private String numeroFacture;
    private LocalDate dateFacture;
    private TypeAvoir typeAvoir = TypeAvoir.CLIENT;
    private Long tiersId;
    private String tiersNom;
    private BigDecimal montantFactureHT = BigDecimal.ZERO;
    private BigDecimal montantFactureTTC = BigDecimal.ZERO;
    private BigDecimal montantDejaAvoirTTC = BigDecimal.ZERO;
    private BigDecimal montantRestantAvoirTTC = BigDecimal.ZERO;
    private List<LigneAvoiriableDTO> lignes = new ArrayList<>();

    public FactureLignesAvoiriablesDTO() {}

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDate getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDate dateFacture) { this.dateFacture = dateFacture; }

    public TypeAvoir getTypeAvoir() { return typeAvoir; }
    public void setTypeAvoir(TypeAvoir typeAvoir) { this.typeAvoir = typeAvoir; }

    public Long getTiersId() { return tiersId; }
    public void setTiersId(Long tiersId) { this.tiersId = tiersId; }

    public String getTiersNom() { return tiersNom; }
    public void setTiersNom(String tiersNom) { this.tiersNom = tiersNom; }

    public BigDecimal getMontantFactureHT() { return montantFactureHT; }
    public void setMontantFactureHT(BigDecimal montantFactureHT) { this.montantFactureHT = montantFactureHT; }

    public BigDecimal getMontantFactureTTC() { return montantFactureTTC; }
    public void setMontantFactureTTC(BigDecimal montantFactureTTC) { this.montantFactureTTC = montantFactureTTC; }

    public BigDecimal getMontantDejaAvoirTTC() { return montantDejaAvoirTTC; }
    public void setMontantDejaAvoirTTC(BigDecimal montantDejaAvoirTTC) { this.montantDejaAvoirTTC = montantDejaAvoirTTC; }

    public BigDecimal getMontantRestantAvoirTTC() { return montantRestantAvoirTTC; }
    public void setMontantRestantAvoirTTC(BigDecimal montantRestantAvoirTTC) { this.montantRestantAvoirTTC = montantRestantAvoirTTC; }

    public List<LigneAvoiriableDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneAvoiriableDTO> lignes) { this.lignes = lignes; }
}
