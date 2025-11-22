package com.ceramique.persistent.dto;

import com.ceramique.persistent.enums.StatutCommande;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CommandeDTO {
    private Long id;
    private String numeroCommande;
    private Long fournisseurId;
    private String fournisseurNom;
    private Long pointDeVenteId;
    private StatutCommande statut;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC+1")
    private LocalDateTime dateCommande;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateLivraisonPrevue;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateLivraisonReelle;
    private BigDecimal montantTotal;
    private String observations;
    private List<LigneCommandeDTO> lignesCommande;

    // Constructors
    public CommandeDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public LocalDateTime getDateLivraisonReelle() { return dateLivraisonReelle; }
    public void setDateLivraisonReelle(LocalDateTime dateLivraisonReelle) { this.dateLivraisonReelle = dateLivraisonReelle; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public List<LigneCommandeDTO> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeDTO> lignesCommande) { this.lignesCommande = lignesCommande; }
}