package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardDTO {

    // --- Ventes ---
    private BigDecimal caAujourdhui = BigDecimal.ZERO;
    private BigDecimal caHier = BigDecimal.ZERO;
    private BigDecimal caMoisEnCours = BigDecimal.ZERO;
    private Long nombreVentesAujourdhui = 0L;
    private Long nombreVentesHier = 0L;
    private Long nombreVentesMoisEnCours = 0L;

    // --- Créances Clients ---
    private BigDecimal totalCreancesClients = BigDecimal.ZERO;
    private BigDecimal totalCreancesEchues = BigDecimal.ZERO;
    private BigDecimal totalCreancesNonEchues = BigDecimal.ZERO;
    private Long nombreFacturesImpayees = 0L;

    // --- Trésorerie & Encaissements ---
    private BigDecimal totalEncaissementsAujourdhui = BigDecimal.ZERO;
    private Map<String, BigDecimal> encaissementsParMode;
    private BigDecimal soldeCaisseActuel = BigDecimal.ZERO;
    private BigDecimal soldeBanqueEstime = BigDecimal.ZERO;

    // --- Dettes Fournisseurs ---
    private BigDecimal totalDettesFournisseurs = BigDecimal.ZERO;

    // --- Stock & Alertes ---
    private Long nombreProduitsEnRupture = 0L;
    private Long nombreProduitsStockBas = 0L;

    // --- Marges & Rentabilité & Résultat Net ---
    private BigDecimal margeMoisEnCours = BigDecimal.ZERO;
    private BigDecimal tauxMargeMoisEnCours = BigDecimal.ZERO;
    private BigDecimal totalDepensesMoisEnCours = BigDecimal.ZERO;
    private BigDecimal resultatNetMoisEnCours = BigDecimal.ZERO;

    // --- Classements ---
    private List<TopClientDTO> topClients;
    private List<TopProduitDTO> topProduits;

    // --- Alertes / Anomalies ---
    private List<AnomalieDTO> alertesRecentes;

    // --- Baromètre de Santé Entreprise ---
    private BarometreEntrepriseDTO barometre;

    public DashboardDTO() {
    }

    public BarometreEntrepriseDTO getBarometre() { return barometre; }
    public void setBarometre(BarometreEntrepriseDTO barometre) { this.barometre = barometre; }

    // Getters and Setters
    public BigDecimal getCaAujourdhui() { return caAujourdhui; }
    public void setCaAujourdhui(BigDecimal caAujourdhui) { this.caAujourdhui = caAujourdhui; }

    public BigDecimal getCaHier() { return caHier; }
    public void setCaHier(BigDecimal caHier) { this.caHier = caHier; }

    public BigDecimal getCaMoisEnCours() { return caMoisEnCours; }
    public void setCaMoisEnCours(BigDecimal caMoisEnCours) { this.caMoisEnCours = caMoisEnCours; }

    public Long getNombreVentesAujourdhui() { return nombreVentesAujourdhui; }
    public void setNombreVentesAujourdhui(Long nombreVentesAujourdhui) { this.nombreVentesAujourdhui = nombreVentesAujourdhui; }

    public Long getNombreVentesHier() { return nombreVentesHier; }
    public void setNombreVentesHier(Long nombreVentesHier) { this.nombreVentesHier = nombreVentesHier; }

    public Long getNombreVentesMoisEnCours() { return nombreVentesMoisEnCours; }
    public void setNombreVentesMoisEnCours(Long nombreVentesMoisEnCours) { this.nombreVentesMoisEnCours = nombreVentesMoisEnCours; }

    public BigDecimal getTotalCreancesClients() { return totalCreancesClients; }
    public void setTotalCreancesClients(BigDecimal totalCreancesClients) { this.totalCreancesClients = totalCreancesClients; }

    public BigDecimal getTotalCreancesEchues() { return totalCreancesEchues; }
    public void setTotalCreancesEchues(BigDecimal totalCreancesEchues) { this.totalCreancesEchues = totalCreancesEchues; }

    public BigDecimal getTotalCreancesNonEchues() { return totalCreancesNonEchues; }
    public void setTotalCreancesNonEchues(BigDecimal totalCreancesNonEchues) { this.totalCreancesNonEchues = totalCreancesNonEchues; }

    public Long getNombreFacturesImpayees() { return nombreFacturesImpayees; }
    public void setNombreFacturesImpayees(Long nombreFacturesImpayees) { this.nombreFacturesImpayees = nombreFacturesImpayees; }

    public BigDecimal getTotalEncaissementsAujourdhui() { return totalEncaissementsAujourdhui; }
    public void setTotalEncaissementsAujourdhui(BigDecimal totalEncaissementsAujourdhui) { this.totalEncaissementsAujourdhui = totalEncaissementsAujourdhui; }

    public Map<String, BigDecimal> getEncaissementsParMode() { return encaissementsParMode; }
    public void setEncaissementsParMode(Map<String, BigDecimal> encaissementsParMode) { this.encaissementsParMode = encaissementsParMode; }

    public BigDecimal getSoldeCaisseActuel() { return soldeCaisseActuel; }
    public void setSoldeCaisseActuel(BigDecimal soldeCaisseActuel) { this.soldeCaisseActuel = soldeCaisseActuel; }

    public BigDecimal getSoldeBanqueEstime() { return soldeBanqueEstime; }
    public void setSoldeBanqueEstime(BigDecimal soldeBanqueEstime) { this.soldeBanqueEstime = soldeBanqueEstime; }

    public BigDecimal getTotalDettesFournisseurs() { return totalDettesFournisseurs; }
    public void setTotalDettesFournisseurs(BigDecimal totalDettesFournisseurs) { this.totalDettesFournisseurs = totalDettesFournisseurs; }

    public Long getNombreProduitsEnRupture() { return nombreProduitsEnRupture; }
    public void setNombreProduitsEnRupture(Long nombreProduitsEnRupture) { this.nombreProduitsEnRupture = nombreProduitsEnRupture; }

    public Long getNombreProduitsStockBas() { return nombreProduitsStockBas; }
    public void setNombreProduitsStockBas(Long nombreProduitsStockBas) { this.nombreProduitsStockBas = nombreProduitsStockBas; }

    public BigDecimal getMargeMoisEnCours() { return margeMoisEnCours; }
    public void setMargeMoisEnCours(BigDecimal margeMoisEnCours) { this.margeMoisEnCours = margeMoisEnCours; }

    public BigDecimal getTauxMargeMoisEnCours() { return tauxMargeMoisEnCours; }
    public void setTauxMargeMoisEnCours(BigDecimal tauxMargeMoisEnCours) { this.tauxMargeMoisEnCours = tauxMargeMoisEnCours; }

    public BigDecimal getTotalDepensesMoisEnCours() { return totalDepensesMoisEnCours; }
    public void setTotalDepensesMoisEnCours(BigDecimal totalDepensesMoisEnCours) { this.totalDepensesMoisEnCours = totalDepensesMoisEnCours; }

    public BigDecimal getResultatNetMoisEnCours() { return resultatNetMoisEnCours; }
    public void setResultatNetMoisEnCours(BigDecimal resultatNetMoisEnCours) { this.resultatNetMoisEnCours = resultatNetMoisEnCours; }

    public List<TopClientDTO> getTopClients() { return topClients; }
    public void setTopClients(List<TopClientDTO> topClients) { this.topClients = topClients; }

    public List<TopProduitDTO> getTopProduits() { return topProduits; }
    public void setTopProduits(List<TopProduitDTO> topProduits) { this.topProduits = topProduits; }

    public List<AnomalieDTO> getAlertesRecentes() { return alertesRecentes; }
    public void setAlertesRecentes(List<AnomalieDTO> alertesRecentes) { this.alertesRecentes = alertesRecentes; }

    // --- Inner DTOs ---

    public static class TopClientDTO {
        private Long clientId;
        private String nom;
        private String nomComplet;
        private BigDecimal totalAchats;
        private Long nombreCommandes;

        public TopClientDTO() {}

        public TopClientDTO(Long clientId, String nom, String nomComplet, BigDecimal totalAchats, Long nombreCommandes) {
            this.clientId = clientId;
            this.nom = nom;
            this.nomComplet = nomComplet;
            this.totalAchats = totalAchats;
            this.nombreCommandes = nombreCommandes;
        }

        public Long getClientId() { return clientId; }
        public void setClientId(Long clientId) { this.clientId = clientId; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getNomComplet() { return nomComplet; }
        public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }

        public BigDecimal getTotalAchats() { return totalAchats; }
        public void setTotalAchats(BigDecimal totalAchats) { this.totalAchats = totalAchats; }

        public Long getNombreCommandes() { return nombreCommandes; }
        public void setNombreCommandes(Long nombreCommandes) { this.nombreCommandes = nombreCommandes; }
    }

    public static class TopProduitDTO {
        private Long produitId;
        private String reference;
        private String designation;
        private BigDecimal quantiteVendue;
        private BigDecimal montantTotal;

        public TopProduitDTO() {}

        public TopProduitDTO(Long produitId, String reference, String designation, BigDecimal quantiteVendue, BigDecimal montantTotal) {
            this.produitId = produitId;
            this.reference = reference;
            this.designation = designation;
            this.quantiteVendue = quantiteVendue;
            this.montantTotal = montantTotal;
        }

        public Long getProduitId() { return produitId; }
        public void setProduitId(Long produitId) { this.produitId = produitId; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }

        public BigDecimal getQuantiteVendue() { return quantiteVendue; }
        public void setQuantiteVendue(BigDecimal quantiteVendue) { this.quantiteVendue = quantiteVendue; }

        public BigDecimal getMontantTotal() { return montantTotal; }
        public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }
    }
}
