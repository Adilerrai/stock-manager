package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutFacture;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FactureAchatDTO {
    private Long id;
    private String numeroFacture;
    private LocalDateTime dateFacture;
    private LocalDateTime dateEcheance;
    private Long fournisseurId;
    private String fournisseurNom;
    private String fournisseurTelephone;
    private BigDecimal montantHt;
    private BigDecimal montantTva;
    private BigDecimal montantTtc;
    private StatutFacture statut;
    private String observations;
    private Long pointDeVenteId;
    private List<LigneFactureAchatDTO> lignes = new ArrayList<>();

    public FactureAchatDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroFacture() { return numeroFacture; }
    public void setNumeroFacture(String numeroFacture) { this.numeroFacture = numeroFacture; }

    public LocalDateTime getDateFacture() { return dateFacture; }
    public void setDateFacture(LocalDateTime dateFacture) { this.dateFacture = dateFacture; }

    public LocalDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDateTime dateEcheance) { this.dateEcheance = dateEcheance; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public String getFournisseurTelephone() { return fournisseurTelephone; }
    public void setFournisseurTelephone(String fournisseurTelephone) { this.fournisseurTelephone = fournisseurTelephone; }

    public BigDecimal getMontantHt() { return montantHt; }
    public void setMontantHt(BigDecimal montantHt) { this.montantHt = montantHt; }

    public BigDecimal getMontantTva() { return montantTva; }
    public void setMontantTva(BigDecimal montantTva) { this.montantTva = montantTva; }

    public BigDecimal getMontantTtc() { return montantTtc; }
    public void setMontantTtc(BigDecimal montantTtc) { this.montantTtc = montantTtc; }

    public StatutFacture getStatut() { return statut; }
    public void setStatut(StatutFacture statut) { this.statut = statut; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneFactureAchatDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneFactureAchatDTO> lignes) { this.lignes = lignes; }
}
