package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutDemandePrix;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DemandePrixDTO {
    private Long id;
    private String numeroDdp;
    private String objet;
    private StatutDemandePrix statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLimiteReponse;
    private String demandeAchatId;
    private String observations;
    private Long pointDeVenteId;
    private List<LigneDemandePrixDTO> lignes = new ArrayList<>();
    private List<OffreFournisseurDTO> offres = new ArrayList<>();
    private List<Long> fournisseurIds = new ArrayList<>();

    public DemandePrixDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroDdp() { return numeroDdp; }
    public void setNumeroDdp(String numeroDdp) { this.numeroDdp = numeroDdp; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public StatutDemandePrix getStatut() { return statut; }
    public void setStatut(StatutDemandePrix statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateLimiteReponse() { return dateLimiteReponse; }
    public void setDateLimiteReponse(LocalDateTime dateLimiteReponse) { this.dateLimiteReponse = dateLimiteReponse; }

    public String getDemandeAchatId() { return demandeAchatId; }
    public void setDemandeAchatId(String demandeAchatId) { this.demandeAchatId = demandeAchatId; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneDemandePrixDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneDemandePrixDTO> lignes) { this.lignes = lignes; }

    public List<OffreFournisseurDTO> getOffres() { return offres; }
    public void setOffres(List<OffreFournisseurDTO> offres) { this.offres = offres; }

    public List<Long> getFournisseurIds() { return fournisseurIds; }
    public void setFournisseurIds(List<Long> fournisseurIds) { this.fournisseurIds = fournisseurIds; }
}
