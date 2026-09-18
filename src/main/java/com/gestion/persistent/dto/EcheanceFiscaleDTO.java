package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EcheanceFiscaleDTO {
    private Long id;
    private Long societeId;
    private String societeNom;
    private String societeIce;
    private String typeEcheance;
    private String libelle;
    private LocalDate dateEcheance;
    private String statut;
    private BigDecimal montantEstime;
    private BigDecimal montantPaye;
    private LocalDate datePaiement;
    private String referencePaiement;
    private String commentaire;

    public EcheanceFiscaleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }

    public String getSocieteNom() { return societeNom; }
    public void setSocieteNom(String societeNom) { this.societeNom = societeNom; }

    public String getSocieteIce() { return societeIce; }
    public void setSocieteIce(String societeIce) { this.societeIce = societeIce; }

    public String getTypeEcheance() { return typeEcheance; }
    public void setTypeEcheance(String typeEcheance) { this.typeEcheance = typeEcheance; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public BigDecimal getMontantEstime() { return montantEstime; }
    public void setMontantEstime(BigDecimal montantEstime) { this.montantEstime = montantEstime; }

    public BigDecimal getMontantPaye() { return montantPaye; }
    public void setMontantPaye(BigDecimal montantPaye) { this.montantPaye = montantPaye; }

    public LocalDate getDatePaiement() { return datePaiement; }
    public void setDatePaiement(LocalDate datePaiement) { this.datePaiement = datePaiement; }

    public String getReferencePaiement() { return referencePaiement; }
    public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }
}
