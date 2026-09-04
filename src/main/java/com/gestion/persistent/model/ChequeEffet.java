package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gestion.persistent.enums.SensEffet;
import com.gestion.persistent.enums.StatutEffet;
import com.gestion.persistent.enums.TypeEffet;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cheques_effets")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChequeEffet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_piece", nullable = false, length = 50)
    private String numeroPiece; // Numéro du chèque ou de l'effet

    @Enumerated(EnumType.STRING)
    @Column(name = "type_effet", nullable = false)
    private TypeEffet typeEffet = TypeEffet.CHEQUE;

    @Enumerated(EnumType.STRING)
    @Column(name = "sens", nullable = false)
    private SensEffet sens = SensEffet.ENCAISSEMENT_CLIENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutEffet statut = StatutEffet.EN_PORTEFEUILLE;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal montant = BigDecimal.ZERO;

    @Column(name = "date_emission")
    private LocalDate dateEmission = LocalDate.now();

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;

    @Column(name = "date_remise")
    private LocalDate dateRemise;

    @Column(name = "date_encaissement")
    private LocalDate dateEncaissement;

    @Column(name = "banque_emettrice")
    private String banqueEmettrice;

    @Column(name = "tireur")
    private String tireur; // Client ou personne ayant émis le chèque

    @Column(name = "beneficiaire")
    private String beneficiaire; // Entreprise ou Fournisseur

    @Column(name = "compte_bancaire_depot")
    private String compteBancaireDepot; // RIB/Compte récepteur

    @Column(name = "reference_paiement")
    private String referencePaiement; // Réf facture ou reçu

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bordereau_remise_id")
    private BordereauRemise bordereauRemise;

    @Column(name = "motif_rejet")
    private String motifRejet;

    @Column(name = "notes")
    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId = 1L;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public ChequeEffet() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPiece() { return numeroPiece; }
    public void setNumeroPiece(String numeroPiece) { this.numeroPiece = numeroPiece; }

    public TypeEffet getTypeEffet() { return typeEffet; }
    public void setTypeEffet(TypeEffet typeEffet) { this.typeEffet = typeEffet; }

    public SensEffet getSens() { return sens; }
    public void setSens(SensEffet sens) { this.sens = sens; }

    public StatutEffet getStatut() { return statut; }
    public void setStatut(StatutEffet statut) { this.statut = statut; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

    public LocalDate getDateRemise() { return dateRemise; }
    public void setDateRemise(LocalDate dateRemise) { this.dateRemise = dateRemise; }

    public LocalDate getDateEncaissement() { return dateEncaissement; }
    public void setDateEncaissement(LocalDate dateEncaissement) { this.dateEncaissement = dateEncaissement; }

    public String getBanqueEmettrice() { return banqueEmettrice; }
    public void setBanqueEmettrice(String banqueEmettrice) { this.banqueEmettrice = banqueEmettrice; }

    public String getTireur() { return tireur; }
    public void setTireur(String tireur) { this.tireur = tireur; }

    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

    public String getCompteBancaireDepot() { return compteBancaireDepot; }
    public void setCompteBancaireDepot(String compteBancaireDepot) { this.compteBancaireDepot = compteBancaireDepot; }

    public String getReferencePaiement() { return referencePaiement; }
    public void setReferencePaiement(String referencePaiement) { this.referencePaiement = referencePaiement; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public Fournisseur getFournisseur() { return fournisseur; }
    public void setFournisseur(Fournisseur fournisseur) { this.fournisseur = fournisseur; }

    public BordereauRemise getBordereauRemise() { return bordereauRemise; }
    public void setBordereauRemise(BordereauRemise bordereauRemise) { this.bordereauRemise = bordereauRemise; }

    public String getMotifRejet() { return motifRejet; }
    public void setMotifRejet(String motifRejet) { this.motifRejet = motifRejet; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
