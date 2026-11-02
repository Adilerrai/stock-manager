package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DocumentLivreurCandidateDTO {

    private Long id;
    private String type; // "BON_LIVRAISON" ou "FACTURE"
    private String numero;
    private String clientNom;
    private String adresse;
    private String telephone;
    private String ville;
    private LocalDateTime dateCreation;
    private BigDecimal montantTotal;
    private String statut;

    public DocumentLivreurCandidateDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}

