package com.gestion.persistent.dto;

import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ChequeEffetDTO {
    private Long id;
    private String numeroPiece;
    private String numeroCheque;
    private TypeEffet typeEffet;
    private SensEffet sens;
    private BigDecimal montant;
    private LocalDate dateEmission;
    private LocalDate dateEcheance;
    private String banqueEmettrice;
    private String tireur;
    private String beneficiaire;
    private StatutEffet statut;
    private String compteBancaireDepot;
    private Long bordereauRemiseId;
    private String numeroBordereau;
    private String bordereauRemiseNumero;
    private LocalDate dateRemise;
    private LocalDate dateEncaissement;
    private String motifRejet;
    private String notes;
    private String referencePaiement;
    private Long clientId;
    private String clientNom;
    private Long fournisseurId;
    private String fournisseurNom;

    public ChequeEffetDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPiece() {
        return numeroPiece != null ? numeroPiece : numeroCheque;
    }
    public void setNumeroPiece(String numeroPiece) {
        this.numeroPiece = numeroPiece;
        if (this.numeroCheque == null) this.numeroCheque = numeroPiece;
    }

    public String getNumeroCheque() {
        return numeroCheque != null ? numeroCheque : numeroPiece;
    }
    public void setNumeroCheque(String numeroCheque) {
        this.numeroCheque = numeroCheque;
        if (this.numeroPiece == null) this.numeroPiece = numeroCheque;
    }

    public TypeEffet getTypeEffet() { return typeEffet; }
    public void setTypeEffet(TypeEffet typeEffet) { this.typeEffet = typeEffet; }

    public SensEffet getSens() { return sens; }
    public void setSens(SensEffet sens) { this.sens = sens; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public String getBanqueEmettrice() { return banqueEmettrice; }
    public void setBanqueEmettrice(String banqueEmettrice) { this.banqueEmettrice = banqueEmettrice; }

    public String getTireur() { return tireur; }
    public void setTireur(String tireur) { this.tireur = tireur; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public StatutEffet getStatut() { return statut; }
    public void setStatut(StatutEffet statut) { this.statut = statut; }

    public String getCompteBancaireDepot() { return compteBancaireDepot; }
    public void setCompteBancaireDepot(String compteBancaireDepot) { this.compteBancaireDepot = compteBancaireDepot; }

    public Long getBordereauRemiseId() { return bordereauRemiseId; }
    public void setBordereauRemiseId(Long bordereauRemiseId) { this.bordereauRemiseId = bordereauRemiseId; }

    public String getNumeroBordereau() {
        return numeroBordereau != null ? numeroBordereau : bordereauRemiseNumero;
    }
    public void setNumeroBordereau(String numeroBordereau) {
        this.numeroBordereau = numeroBordereau;
        if (this.bordereauRemiseNumero == null) this.bordereauRemiseNumero = numeroBordereau;
    }

    public String getBordereauRemiseNumero() {
        return bordereauRemiseNumero != null ? bordereauRemiseNumero : numeroBordereau;
    }
    public void setBordereauRemiseNumero(String bordereauRemiseNumero) {
        this.bordereauRemiseNumero = bordereauRemiseNumero;
        if (this.numeroBordereau == null) this.numeroBordereau = bordereauRemiseNumero;
    }

    public LocalDate getDateRemise() { return dateRemise; }
    public void setDateRemise(LocalDate dateRemise) { this.dateRemise = dateRemise; }

    public LocalDate getDateEncaissement() { return dateEncaissement; }
    public void setDateEncaissement(LocalDate dateEncaissement) { this.dateEncaissement = dateEncaissement; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getReferencePaiement() { return referencePaiement; }
    public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientNom() { return clientNom; }
    public void setClientNom(String clientNom) { this.clientNom = clientNom; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }
}
