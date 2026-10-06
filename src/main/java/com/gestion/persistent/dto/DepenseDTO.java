package com.gestion.persistent.dto;

import com.gestion.persistent.enums.CategorieDepense;
import com.gestion.persistent.enums.ModePaiement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DepenseDTO {
    private Long id;
    private String reference;
    private String designation;
    private BigDecimal montant;
    private LocalDate dateDepense;
    private CategorieDepense categorie;
    private String categorieLibelle;
    private ModePaiement modePaiement;
    private String beneficiaire;
    private Long fournisseurId;
    private String fournisseurNom;
    private String fournisseurTelephone;
    private String fournisseurIce;
    private String articlesDetail;
    private String numeroFactureJustificatif;
    private String notes;
    private Boolean deverseeCompta = false;
    private LocalDateTime dateDeversement;
    private Long ecritureId;
    private Long creeParUserId;
    private String creeParNom;
    private Long pointDeVenteId;
    private LocalDateTime dateCreation;

    public DepenseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateDepense() { return dateDepense; }
    public void setDateDepense(LocalDate dateDepense) { this.dateDepense = dateDepense; }

    public CategorieDepense getCategorie() { return categorie; }
    public void setCategorie(CategorieDepense categorie) {
        this.categorie = categorie;
        if (categorie != null) {
            this.categorieLibelle = categorie.getLibelle();
        }
    }

    public String getCategorieLibelle() { return categorieLibelle; }
    public void setCategorieLibelle(String categorieLibelle) { this.categorieLibelle = categorieLibelle; }

    public ModePaiement getModePaiement() { return modePaiement; }
    public void setModePaiement(ModePaiement modePaiement) { this.modePaiement = modePaiement; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public String getNumeroFactureJustificatif() { return numeroFactureJustificatif; }
    public void setNumeroFactureJustificatif(String numeroFactureJustificatif) { this.numeroFactureJustificatif = numeroFactureJustificatif; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getCreeParUserId() { return creeParUserId; }
    public void setCreeParUserId(Long creeParUserId) { this.creeParUserId = creeParUserId; }

    public String getCreeParNom() { return creeParNom; }
    public void setCreeParNom(String creeParNom) { this.creeParNom = creeParNom; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public String getFournisseurTelephone() { return fournisseurTelephone; }
    public void setFournisseurTelephone(String fournisseurTelephone) { this.fournisseurTelephone = fournisseurTelephone; }

    public String getFournisseurIce() { return fournisseurIce; }
    public void setFournisseurIce(String fournisseurIce) { this.fournisseurIce = fournisseurIce; }

    public String getArticlesDetail() { return articlesDetail; }
    public void setArticlesDetail(String articlesDetail) { this.articlesDetail = articlesDetail; }

    public Boolean getDeverseeCompta() { return deverseeCompta; }
    public void setDeverseeCompta(Boolean deverseeCompta) { this.deverseeCompta = deverseeCompta; }

    public LocalDateTime getDateDeversement() { return dateDeversement; }
    public void setDateDeversement(LocalDateTime dateDeversement) { this.dateDeversement = dateDeversement; }

    public Long getEcritureId() { return ecritureId; }
    public void setEcritureId(Long ecritureId) { this.ecritureId = ecritureId; }
}
