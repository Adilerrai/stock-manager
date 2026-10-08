package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CockpitRentabiliteDTO {
    private MargeDTO cascadeGlobale;
    private List<EvolutionMargeDTO> evolutionMensuelle = new ArrayList<>();
    private List<StatistiqueMotifRetourDTO> motifsAvoirs = new ArrayList<>();
    private List<MargeDTO.LigneMargeDTO> topProduitsRentables = new ArrayList<>();
    private List<MargeDTO.LigneMargeDTO> flopProduitsPerte = new ArrayList<>();
    private List<MargeDTO.LigneMargeDTO> topClientsRentables = new ArrayList<>();
    private List<MargeDTO.LigneMargeDTO> clientsRisque = new ArrayList<>();
    private List<MargeDTO.LigneMargeDTO> margesParFamille = new ArrayList<>();
    private AlertesRentabiliteDTO alertes = new AlertesRentabiliteDTO();

    public CockpitRentabiliteDTO() {}

    public MargeDTO getCascadeGlobale() { return cascadeGlobale; }
    public void setCascadeGlobale(MargeDTO cascadeGlobale) { this.cascadeGlobale = cascadeGlobale; }

    public List<EvolutionMargeDTO> getEvolutionMensuelle() { return evolutionMensuelle; }
    public void setEvolutionMensuelle(List<EvolutionMargeDTO> evolutionMensuelle) { this.evolutionMensuelle = evolutionMensuelle; }

    public List<StatistiqueMotifRetourDTO> getMotifsAvoirs() { return motifsAvoirs; }
    public void setMotifsAvoirs(List<StatistiqueMotifRetourDTO> motifsAvoirs) { this.motifsAvoirs = motifsAvoirs; }

    public List<MargeDTO.LigneMargeDTO> getTopProduitsRentables() { return topProduitsRentables; }
    public void setTopProduitsRentables(List<MargeDTO.LigneMargeDTO> topProduitsRentables) { this.topProduitsRentables = topProduitsRentables; }

    public List<MargeDTO.LigneMargeDTO> getFlopProduitsPerte() { return flopProduitsPerte; }
    public void setFlopProduitsPerte(List<MargeDTO.LigneMargeDTO> flopProduitsPerte) { this.flopProduitsPerte = flopProduitsPerte; }

    public List<MargeDTO.LigneMargeDTO> getTopClientsRentables() { return topClientsRentables; }
    public void setTopClientsRentables(List<MargeDTO.LigneMargeDTO> topClientsRentables) { this.topClientsRentables = topClientsRentables; }

    public List<MargeDTO.LigneMargeDTO> getClientsRisque() { return clientsRisque; }
    public void setClientsRisque(List<MargeDTO.LigneMargeDTO> clientsRisque) { this.clientsRisque = clientsRisque; }

    public List<MargeDTO.LigneMargeDTO> getMargesParFamille() { return margesParFamille; }
    public void setMargesParFamille(List<MargeDTO.LigneMargeDTO> margesParFamille) { this.margesParFamille = margesParFamille; }

    public AlertesRentabiliteDTO getAlertes() { return alertes; }
    public void setAlertes(AlertesRentabiliteDTO alertes) { this.alertes = alertes; }

    public static class AlertesRentabiliteDTO {
        private int nbProduitsMargeNegative = 0;
        private int nbProduitsSousPmp = 0;
        private int nbProduitsFaibleMarge = 0;
        private int nbClientsAvoirsAnormaux = 0;
        private BigDecimal perteEstimeeAvoirs = BigDecimal.ZERO;

        public AlertesRentabiliteDTO() {}

        public int getNbProduitsMargeNegative() { return nbProduitsMargeNegative; }
        public void setNbProduitsMargeNegative(int nbProduitsMargeNegative) { this.nbProduitsMargeNegative = nbProduitsMargeNegative; }

        public int getNbProduitsSousPmp() { return nbProduitsSousPmp; }
        public void setNbProduitsSousPmp(int nbProduitsSousPmp) { this.nbProduitsSousPmp = nbProduitsSousPmp; }

        public int getNbProduitsFaibleMarge() { return nbProduitsFaibleMarge; }
        public void setNbProduitsFaibleMarge(int nbProduitsFaibleMarge) { this.nbProduitsFaibleMarge = nbProduitsFaibleMarge; }

        public int getNbClientsAvoirsAnormaux() { return nbClientsAvoirsAnormaux; }
        public void setNbClientsAvoirsAnormaux(int nbClientsAvoirsAnormaux) { this.nbClientsAvoirsAnormaux = nbClientsAvoirsAnormaux; }

        public BigDecimal getPerteEstimeeAvoirs() { return perteEstimeeAvoirs; }
        public void setPerteEstimeeAvoirs(BigDecimal perteEstimeeAvoirs) { this.perteEstimeeAvoirs = perteEstimeeAvoirs; }
    }
}
