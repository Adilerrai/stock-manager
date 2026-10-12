package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutOffreFournisseur;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OffreFournisseurDTO {
    private Long id;
    private Long demandePrixId;
    private Long fournisseurId;
    private String fournisseurNom;
    private String numeroDevis;
    private LocalDateTime dateReception;
    private Integer delaiLivraisonJours;
    private BigDecimal fraisPort;
    private StatutOffreFournisseur statut;
    private String conditionsPaiement;
    private String observations;
    private BigDecimal totalHT;
    private List<LigneOffreFournisseurDTO> lignes = new ArrayList<>();

    public OffreFournisseurDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandePrixId() { return demandePrixId; }
    public void setDemandePrixId(Long demandePrixId) { this.demandePrixId = demandePrixId; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public LocalDateTime getDateReception() { return dateReception; }
    public void setDateReception(LocalDateTime dateReception) { this.dateReception = dateReception; }

    public Integer getDelaiLivraisonJours() { return delaiLivraisonJours; }
    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) { this.delaiLivraisonJours = delaiLivraisonJours; }

    public BigDecimal getFraisPort() { return fraisPort; }
    public void setFraisPort(BigDecimal fraisPort) { this.fraisPort = fraisPort; }

    public StatutOffreFournisseur getStatut() { return statut; }
    public void setStatut(StatutOffreFournisseur statut) { this.statut = statut; }

    public String getConditionsPaiement() { return conditionsPaiement; }
    public void setConditionsPaiement(String conditionsPaiement) { this.conditionsPaiement = conditionsPaiement; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public BigDecimal getTotalHT() { return totalHT; }
    public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }

    public List<LigneOffreFournisseurDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneOffreFournisseurDTO> lignes) { this.lignes = lignes; }
}
