package com.gestion.persistent.dto;

import com.gestion.persistent.enums.Incoterm;
import com.gestion.persistent.enums.StatutCouts;
import com.gestion.persistent.enums.TypeAchat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class RecapCoutRevientDTO {

    private Long commandeId;
    private String numeroCommande;
    private TypeAchat typeAchat;
    private Incoterm incoterm;
    private String incotermLibelle;
    private String devise;
    private BigDecimal tauxChangeApplique;
    private String sourceTauxChange; // "DUM" ou "ESTIME_COMMANDE"
    private StatutCouts statutCouts;

    // Décomposition financière (en MAD)
    private BigDecimal montantMarchandiseDevise = BigDecimal.ZERO;
    private BigDecimal montantMarchandiseMad = BigDecimal.ZERO;

    // Éléments d'acheminement (vers CIF)
    private BigDecimal transportOrigineMad = BigDecimal.ZERO;
    private BigDecimal fretInternationalMad = BigDecimal.ZERO;
    private BigDecimal totalCfrMad = BigDecimal.ZERO;
    private BigDecimal assuranceMad = BigDecimal.ZERO;
    private BigDecimal valeurCifMad = BigDecimal.ZERO;

    // Dédouanement
    private BigDecimal droitsDouaneMad = BigDecimal.ZERO;
    private BigDecimal transitaireMad = BigDecimal.ZERO;
    private BigDecimal totalDouaneMad = BigDecimal.ZERO;

    // Charges locales
    private BigDecimal magasinageMad = BigDecimal.ZERO;
    private BigDecimal penalitesMad = BigDecimal.ZERO;
    private BigDecimal transportLocalMad = BigDecimal.ZERO;
    private BigDecimal manutentionMad = BigDecimal.ZERO;
    private BigDecimal mainOeuvreMad = BigDecimal.ZERO;
    private BigDecimal fraisBancairesMad = BigDecimal.ZERO;
    private BigDecimal autresChargesMad = BigDecimal.ZERO;
    private BigDecimal totalChargesLocalesMad = BigDecimal.ZERO;

    // Totaux
    private BigDecimal totalChargesEstimeesMad = BigDecimal.ZERO;
    private BigDecimal totalChargesReellesMad = BigDecimal.ZERO;
    private BigDecimal totalChargesIncorporablesMad = BigDecimal.ZERO;
    private BigDecimal coutRevientTotalMad = BigDecimal.ZERO;
    private BigDecimal coefficientApproche = BigDecimal.ONE;

    // Hors coût (information fiscale / trésorerie)
    private BigDecimal tvaImportRecuperableMad = BigDecimal.ZERO;
    private BigDecimal chargesNonIncorporablesMad = BigDecimal.ZERO;

    private List<LigneCoutRevientDTO> lignes = new ArrayList<>();
    private List<ChargeAchatDTO> charges = new ArrayList<>();

    public RecapCoutRevientDTO() {}

    public Long getCommandeId() { return commandeId; }
    public void setCommandeId(Long commandeId) { this.commandeId = commandeId; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public TypeAchat getTypeAchat() { return typeAchat; }
    public void setTypeAchat(TypeAchat typeAchat) { this.typeAchat = typeAchat; }

    public Incoterm getIncoterm() { return incoterm; }
    public void setIncoterm(Incoterm incoterm) {
        this.incoterm = incoterm;
        if (incoterm != null) this.incotermLibelle = incoterm.getLibelle();
    }

    public String getIncotermLibelle() { return incotermLibelle; }
    public void setIncotermLibelle(String incotermLibelle) { this.incotermLibelle = incotermLibelle; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public BigDecimal getTauxChangeApplique() { return tauxChangeApplique; }
    public void setTauxChangeApplique(BigDecimal tauxChangeApplique) { this.tauxChangeApplique = tauxChangeApplique; }

    public String getSourceTauxChange() { return sourceTauxChange; }
    public void setSourceTauxChange(String sourceTauxChange) { this.sourceTauxChange = sourceTauxChange; }

    public StatutCouts getStatutCouts() { return statutCouts; }
    public void setStatutCouts(StatutCouts statutCouts) { this.statutCouts = statutCouts; }

    public BigDecimal getMontantMarchandiseDevise() { return montantMarchandiseDevise; }
    public void setMontantMarchandiseDevise(BigDecimal montantMarchandiseDevise) { this.montantMarchandiseDevise = montantMarchandiseDevise; }

    public BigDecimal getMontantMarchandiseMad() { return montantMarchandiseMad; }
    public void setMontantMarchandiseMad(BigDecimal montantMarchandiseMad) { this.montantMarchandiseMad = montantMarchandiseMad; }

    public BigDecimal getTransportOrigineMad() { return transportOrigineMad; }
    public void setTransportOrigineMad(BigDecimal transportOrigineMad) { this.transportOrigineMad = transportOrigineMad; }

    public BigDecimal getFretInternationalMad() { return fretInternationalMad; }
    public void setFretInternationalMad(BigDecimal fretInternationalMad) { this.fretInternationalMad = fretInternationalMad; }

    public BigDecimal getTotalCfrMad() { return totalCfrMad; }
    public void setTotalCfrMad(BigDecimal totalCfrMad) { this.totalCfrMad = totalCfrMad; }

    public BigDecimal getAssuranceMad() { return assuranceMad; }
    public void setAssuranceMad(BigDecimal assuranceMad) { this.assuranceMad = assuranceMad; }

    public BigDecimal getValeurCifMad() { return valeurCifMad; }
    public void setValeurCifMad(BigDecimal valeurCifMad) { this.valeurCifMad = valeurCifMad; }

    public BigDecimal getDroitsDouaneMad() { return droitsDouaneMad; }
    public void setDroitsDouaneMad(BigDecimal droitsDouaneMad) { this.droitsDouaneMad = droitsDouaneMad; }

    public BigDecimal getTransitaireMad() { return transitaireMad; }
    public void setTransitaireMad(BigDecimal transitaireMad) { this.transitaireMad = transitaireMad; }

    public BigDecimal getTotalDouaneMad() { return totalDouaneMad; }
    public void setTotalDouaneMad(BigDecimal totalDouaneMad) { this.totalDouaneMad = totalDouaneMad; }

    public BigDecimal getMagasinageMad() { return magasinageMad; }
    public void setMagasinageMad(BigDecimal magasinageMad) { this.magasinageMad = magasinageMad; }

    public BigDecimal getPenalitesMad() { return penalitesMad; }
    public void setPenalitesMad(BigDecimal penalitesMad) { this.penalitesMad = penalitesMad; }

    public BigDecimal getTransportLocalMad() { return transportLocalMad; }
    public void setTransportLocalMad(BigDecimal transportLocalMad) { this.transportLocalMad = transportLocalMad; }

    public BigDecimal getManutentionMad() { return manutentionMad; }
    public void setManutentionMad(BigDecimal manutentionMad) { this.manutentionMad = manutentionMad; }

    public BigDecimal getMainOeuvreMad() { return mainOeuvreMad; }
    public void setMainOeuvreMad(BigDecimal mainOeuvreMad) { this.mainOeuvreMad = mainOeuvreMad; }

    public BigDecimal getFraisBancairesMad() { return fraisBancairesMad; }
    public void setFraisBancairesMad(BigDecimal fraisBancairesMad) { this.fraisBancairesMad = fraisBancairesMad; }

    public BigDecimal getAutresChargesMad() { return autresChargesMad; }
    public void setAutresChargesMad(BigDecimal autresChargesMad) { this.autresChargesMad = autresChargesMad; }

    public BigDecimal getTotalChargesLocalesMad() { return totalChargesLocalesMad; }
    public void setTotalChargesLocalesMad(BigDecimal totalChargesLocalesMad) { this.totalChargesLocalesMad = totalChargesLocalesMad; }

    public BigDecimal getTotalChargesEstimeesMad() { return totalChargesEstimeesMad; }
    public void setTotalChargesEstimeesMad(BigDecimal totalChargesEstimeesMad) { this.totalChargesEstimeesMad = totalChargesEstimeesMad; }

    public BigDecimal getTotalChargesReellesMad() { return totalChargesReellesMad; }
    public void setTotalChargesReellesMad(BigDecimal totalChargesReellesMad) { this.totalChargesReellesMad = totalChargesReellesMad; }

    public BigDecimal getTotalChargesIncorporablesMad() { return totalChargesIncorporablesMad; }
    public void setTotalChargesIncorporablesMad(BigDecimal totalChargesIncorporablesMad) { this.totalChargesIncorporablesMad = totalChargesIncorporablesMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }

    public BigDecimal getCoefficientApproche() { return coefficientApproche; }
    public void setCoefficientApproche(BigDecimal coefficientApproche) { this.coefficientApproche = coefficientApproche; }

    public BigDecimal getTvaImportRecuperableMad() { return tvaImportRecuperableMad; }
    public void setTvaImportRecuperableMad(BigDecimal tvaImportRecuperableMad) { this.tvaImportRecuperableMad = tvaImportRecuperableMad; }

    public BigDecimal getChargesNonIncorporablesMad() { return chargesNonIncorporablesMad; }
    public void setChargesNonIncorporablesMad(BigDecimal chargesNonIncorporablesMad) { this.chargesNonIncorporablesMad = chargesNonIncorporablesMad; }

    public List<LigneCoutRevientDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneCoutRevientDTO> lignes) { this.lignes = lignes; }

    public List<ChargeAchatDTO> getCharges() { return charges; }
    public void setCharges(List<ChargeAchatDTO> charges) { this.charges = charges; }
}
