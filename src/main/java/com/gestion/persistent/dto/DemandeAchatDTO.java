package com.gestion.persistent.dto;

import com.gestion.persistent.enums.PrioriteDemandeAchat;
import com.gestion.persistent.enums.StatutDemandeAchat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DemandeAchatDTO {
    private Long id;
    private String numeroDa;
    private String demandeurNom;
    private String serviceDemandeur;
    private String objet;
    private StatutDemandeAchat statut = StatutDemandeAchat.BROUILLON;
    private PrioriteDemandeAchat priorite = PrioriteDemandeAchat.NORMALE;
    private LocalDateTime dateDemande;
    private LocalDateTime dateSouhaitee;
    private String observations;
    private Long demandePrixId;
    private String numeroDdpAssociee;
    private List<Long> ddpIds = new ArrayList<>();
    private List<String> numerosDdpAssociees = new ArrayList<>();
    private Long pointDeVenteId;
    private List<LigneDemandeAchatDTO> lignes = new ArrayList<>();

    public DemandeAchatDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroDa() { return numeroDa; }
    public void setNumeroDa(String numeroDa) { this.numeroDa = numeroDa; }

    public String getDemandeurNom() { return demandeurNom; }
    public void setDemandeurNom(String demandeurNom) { this.demandeurNom = demandeurNom; }

    public String getServiceDemandeur() { return serviceDemandeur; }
    public void setServiceDemandeur(String serviceDemandeur) { this.serviceDemandeur = serviceDemandeur; }

    public String getObjet() { return objet; }
    public void setObjet(String objet) { this.objet = objet; }

    public StatutDemandeAchat getStatut() { return statut; }
    public void setStatut(StatutDemandeAchat statut) { this.statut = statut; }

    public PrioriteDemandeAchat getPriorite() { return priorite; }
    public void setPriorite(PrioriteDemandeAchat priorite) { this.priorite = priorite; }

    public LocalDateTime getDateDemande() { return dateDemande; }
    public void setDateDemande(LocalDateTime dateDemande) { this.dateDemande = dateDemande; }

    public LocalDateTime getDateSouhaitee() { return dateSouhaitee; }
    public void setDateSouhaitee(LocalDateTime dateSouhaitee) { this.dateSouhaitee = dateSouhaitee; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getDemandePrixId() { return demandePrixId; }
    public void setDemandePrixId(Long demandePrixId) { this.demandePrixId = demandePrixId; }

    public String getNumeroDdpAssociee() { return numeroDdpAssociee; }
    public void setNumeroDdpAssociee(String numeroDdpAssociee) { this.numeroDdpAssociee = numeroDdpAssociee; }

    public List<Long> getDdpIds() { return ddpIds; }
    public void setDdpIds(List<Long> ddpIds) { this.ddpIds = ddpIds; }

    public List<String> getNumerosDdpAssociees() { return numerosDdpAssociees; }
    public void setNumerosDdpAssociees(List<String> numerosDdpAssociees) { this.numerosDdpAssociees = numerosDdpAssociees; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public List<LigneDemandeAchatDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneDemandeAchatDTO> lignes) { this.lignes = lignes; }
}
