package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutLivraison;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BonLivraisonClientDTO {
    private Long id;
    private String numeroBl;
    private LocalDateTime dateBl;
    private Long clientId;
    private String clientNom;
    private String clientTelephone;
    private Long commandeClientId;
    private String commandeClientNumero;
    private String numeroCommandeExterne;
    private String bcClient;
    private List<Long> commandeClientIds = new ArrayList<>();
    private List<String> commandeClientNumeros = new ArrayList<>();
    private StatutLivraison statut;
    private Long livreurId;
    private String livreurNom;
    private LocalDateTime dateExpedition;
    private LocalDateTime dateLivraisonEffective;
    private String commentaireLivreur;
    private BigDecimal remiseGlobalePourcentage;
    private BigDecimal remiseGlobaleMontant;
    private BigDecimal montantTotal;
    private BigDecimal montantHT;
    private BigDecimal tauxTVA = BigDecimal.valueOf(20);
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private String observations;
    private Long pointDeVenteId;
    private Long factureId;
    private String factureNumero;
    private Boolean facturé;
    private FactureDTO facture;
    private Long depotId;
    private String depotNom;
    private List<LigneBonLivraisonClientDTO> lignes = new ArrayList<>();
    private List<DocumentComptableDTO> piecesJointes = new ArrayList<>();

    public BonLivraisonClientDTO() {}

    public Long getDepotId() {
        if (depotId != null) return depotId;
        if (lignes != null && !lignes.isEmpty() && lignes.get(0).getDepotId() != null) {
            return lignes.get(0).getDepotId();
        }
        return null;
    }
    public void setDepotId(Long depotId) { this.depotId = depotId; }

    public String getDepotNom() {
        if (depotNom != null) return depotNom;
        if (lignes != null && !lignes.isEmpty() && lignes.get(0).getDepotNom() != null) {
            return lignes.get(0).getDepotNom();
        }
        return null;
    }
    public void setDepotNom(String depotNom) { this.depotNom = depotNom; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public LocalDateTime getDateBl() { return dateBl; }
    public void setDateBl(LocalDateTime dateBl) { this.dateBl = dateBl; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getClientTelephone() { return clientTelephone; }
    public void setClientTelephone(String clientTelephone) { this.clientTelephone = clientTelephone; }

    public Long getCommandeClientId() { return commandeClientId; }
    public void setCommandeClientId(Long commandeClientId) { this.commandeClientId = commandeClientId; }

    public String getCommandeClientNumero() { return commandeClientNumero; }
    public void setCommandeClientNumero(String commandeClientNumero) { this.commandeClientNumero = commandeClientNumero; }

    public String getNumeroCommandeExterne() { return numeroCommandeExterne != null ? numeroCommandeExterne : bcClient; }
    public void setNumeroCommandeExterne(String numeroCommandeExterne) {
        this.numeroCommandeExterne = numeroCommandeExterne;
        if (this.bcClient == null) this.bcClient = numeroCommandeExterne;
    }

    public String getBcClient() { return bcClient != null ? bcClient : numeroCommandeExterne; }
    public void setBcClient(String bcClient) {
        this.bcClient = bcClient;
        if (this.numeroCommandeExterne == null) this.numeroCommandeExterne = bcClient;
    }

    public StatutLivraison getStatut() { return statut; }
    public void setStatut(StatutLivraison statut) { this.statut = statut; }

    public Long getLivreurId() { return livreurId; }
    public void setLivreurId(Long livreurId) { this.livreurId = livreurId; }

    public String getLivreurNom() { return livreurNom; }
    public void setLivreurNom(String livreurNom) { this.livreurNom = livreurNom; }

    public LocalDateTime getDateExpedition() { return dateExpedition; }
    public void setDateExpedition(LocalDateTime dateExpedition) { this.dateExpedition = dateExpedition; }

    public LocalDateTime getDateLivraisonEffective() { return dateLivraisonEffective; }
    public void setDateLivraisonEffective(LocalDateTime dateLivraisonEffective) { this.dateLivraisonEffective = dateLivraisonEffective; }

    public String getCommentaireLivreur() { return commentaireLivreur; }
    public void setCommentaireLivreur(String commentaireLivreur) { this.commentaireLivreur = commentaireLivreur; }

    public BigDecimal getRemiseGlobalePourcentage() { return remiseGlobalePourcentage != null ? remiseGlobalePourcentage : BigDecimal.ZERO; }
    public void setRemiseGlobalePourcentage(BigDecimal remiseGlobalePourcentage) { this.remiseGlobalePourcentage = remiseGlobalePourcentage; }

    public BigDecimal getRemiseGlobaleMontant() { return remiseGlobaleMontant != null ? remiseGlobaleMontant : BigDecimal.ZERO; }
    public void setRemiseGlobaleMontant(BigDecimal remiseGlobaleMontant) { this.remiseGlobaleMontant = remiseGlobaleMontant; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public BigDecimal getMontantHT() {
        return montantHT != null ? montantHT : (montantTotal != null ? montantTotal : BigDecimal.ZERO);
    }
    public void setMontantHT(BigDecimal montantHT) {
        this.montantHT = montantHT;
    }

    public BigDecimal getTauxTVA() {
        return tauxTVA != null ? tauxTVA : BigDecimal.valueOf(20);
    }
    public void setTauxTVA(BigDecimal tauxTVA) {
        this.tauxTVA = tauxTVA;
    }

    public BigDecimal getMontantTVA() {
        if (montantTVA != null) return montantTVA;
        BigDecimal ht = getMontantHT();
        BigDecimal taux = getTauxTVA();
        return ht.multiply(taux).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
    public void setMontantTVA(BigDecimal montantTVA) {
        this.montantTVA = montantTVA;
    }

    public BigDecimal getMontantTTC() {
        if (montantTTC != null) return montantTTC;
        return getMontantHT().add(getMontantTVA());
    }
    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public Long getFactureId() { return factureId; }
    public void setFactureId(Long factureId) { this.factureId = factureId; }

    public String getFactureNumero() { return factureNumero; }
    public void setFactureNumero(String factureNumero) { this.factureNumero = factureNumero; }

    public Boolean getFacturé() { return facturé != null ? facturé : (factureId != null); }
    public void setFacturé(Boolean facturé) { this.facturé = facturé; }

    public FactureDTO getFacture() { return facture; }
    public void setFacture(FactureDTO facture) { this.facture = facture; }

    public List<LigneBonLivraisonClientDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneBonLivraisonClientDTO> lignes) { this.lignes = lignes; }

    public List<Long> getCommandeClientIds() { return commandeClientIds; }
    public void setCommandeClientIds(List<Long> commandeClientIds) { this.commandeClientIds = commandeClientIds; }

    public List<String> getCommandeClientNumeros() { return commandeClientNumeros; }
    public void setCommandeClientNumeros(List<String> commandeClientNumeros) { this.commandeClientNumeros = commandeClientNumeros; }

    public List<DocumentComptableDTO> getPiecesJointes() { return piecesJointes; }
    public void setPiecesJointes(List<DocumentComptableDTO> piecesJointes) { this.piecesJointes = piecesJointes != null ? piecesJointes : new ArrayList<>(); }
}
