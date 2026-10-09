package com.gestion.persistent.dto;

import com.gestion.persistent.enums.NatureAvoir;
import com.gestion.persistent.enums.TypeAvoir;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreerAvoirPartielDTO {
    private Long factureId;
    private TypeAvoir typeAvoir = TypeAvoir.CLIENT;
    private NatureAvoir natureAvoir = NatureAvoir.RETOUR_MARCHANDISE;
    private LocalDate dateAvoir = LocalDate.now();
    private String motif;
    private String notes;
    private Long depotId;
    private List<LigneAvoirPartielDTO> lignes = new ArrayList<>();

    // Pour avoir commercial forfaitaire sur montant global (sans produit)
    private BigDecimal montantHTForfaitaire;
    private BigDecimal tauxTVAForfaitaire = BigDecimal.valueOf(20);
    private String libelleForfaitaire;

    public CreerAvoirPartielDTO() {}

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public TypeAvoir getTypeAvoir() { return typeAvoir; }
    public void setTypeAvoir(TypeAvoir typeAvoir) { this.typeAvoir = typeAvoir; }

    public NatureAvoir getNatureAvoir() { return natureAvoir; }
    public void setNatureAvoir(NatureAvoir natureAvoir) { this.natureAvoir = natureAvoir; }

    public LocalDate getDateAvoir() { return dateAvoir; }
    public void setDateAvoir(LocalDate dateAvoir) { this.dateAvoir = dateAvoir; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getDepotId() { return depotId; }
    public void setDepotId(Long depotId) { this.depotId = depotId; }

    public List<LigneAvoirPartielDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneAvoirPartielDTO> lignes) { this.lignes = lignes; }

    public BigDecimal getMontantHTForfaitaire() { return montantHTForfaitaire; }
    public void setMontantHTForfaitaire(BigDecimal montantHTForfaitaire) { this.montantHTForfaitaire = montantHTForfaitaire; }

    public BigDecimal getTauxTVAForfaitaire() { return tauxTVAForfaitaire; }
    public void setTauxTVAForfaitaire(BigDecimal tauxTVAForfaitaire) { this.tauxTVAForfaitaire = tauxTVAForfaitaire; }

    public String getLibelleForfaitaire() { return libelleForfaitaire; }
    public void setLibelleForfaitaire(String libelleForfaitaire) { this.libelleForfaitaire = libelleForfaitaire; }
}
