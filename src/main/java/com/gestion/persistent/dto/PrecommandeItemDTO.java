package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutPrecommande;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PrecommandeItemDTO {
    private Long ligneDevisId;
    private Long devisId;
    private String numeroDevis;
    private LocalDate dateDevis;
    private Long clientId;
    private String clientNom;
    private Long produitId;
    private String reference;
    private String designation;
    private BigDecimal quantite;
    private String unite = "U";
    private String etatStock;
    private Boolean disponibleEnStock;
    private BigDecimal stockDisponible;
    private StatutPrecommande statutPrecommande;
    private Long demandeAchatId;
    private String numeroDa;
    private LocalDateTime dateAjoutPrecommande;

    public PrecommandeItemDTO() {}

    public Long getLigneDevisId() { return ligneDevisId; }
    public void setLigneDevisId(Long ligneDevisId) { this.ligneDevisId = ligneDevisId; }

    public Long getDevisId() { return devisId; }
    public void setDevisId(Long devisId) { this.devisId = devisId; }

    public String getNumeroDevis() { return numeroDevis; }
    public void setNumeroDevis(String numeroDevis) { this.numeroDevis = numeroDevis; }

    public LocalDate getDateDevis() { return dateDevis; }
    public void setDateDevis(LocalDate dateDevis) { this.dateDevis = dateDevis; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getQuantite() { return quantite; }
    public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public String getEtatStock() { return etatStock; }
    public void setEtatStock(String etatStock) { this.etatStock = etatStock; }

    public Boolean getDisponibleEnStock() { return disponibleEnStock; }
    public void setDisponibleEnStock(Boolean disponibleEnStock) { this.disponibleEnStock = disponibleEnStock; }

    public BigDecimal getStockDisponible() { return stockDisponible; }
    public void setStockDisponible(BigDecimal stockDisponible) { this.stockDisponible = stockDisponible; }

    public StatutPrecommande getStatutPrecommande() { return statutPrecommande; }
    public void setStatutPrecommande(StatutPrecommande statutPrecommande) { this.statutPrecommande = statutPrecommande; }

    public Long getDemandeAchatId() { return demandeAchatId; }
    public void setDemandeAchatId(Long demandeAchatId) { this.demandeAchatId = demandeAchatId; }

    public String getNumeroDa() { return numeroDa; }
    public void setNumeroDa(String numeroDa) { this.numeroDa = numeroDa; }

    public LocalDateTime getDateAjoutPrecommande() { return dateAjoutPrecommande; }
    public void setDateAjoutPrecommande(LocalDateTime dateAjoutPrecommande) { this.dateAjoutPrecommande = dateAjoutPrecommande; }
}
