package com.gestion.persistent.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "echeances_fiscales")
public class EcheanceFiscale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "societe_id", nullable = false)
    private Societe societe;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "type_echeance", nullable = false, length = 50)
    private String typeEcheance; // TVA_MENSUELLE, IS_ACOMPTE_1, IS_ACOMPTE_2, IS_ACOMPTE_3, IS_ACOMPTE_4, IS_REGULARISATION, LIASSE_FISCALE, CNSS_MENSUELLE

    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance;

    @Column(name = "statut", nullable = false, length = 30)
    private String statut = "A_FAIRE"; // A_FAIRE, EN_COURS, FAIT, EN_RETARD

    @Column(name = "montant_estime", precision = 15, scale = 2)
    private BigDecimal montantEstime = BigDecimal.ZERO;

    @Column(name = "montant_paye", precision = 15, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    @Column(name = "date_paiement")
    private LocalDate datePaiement;

    @Column(name = "reference_paiement", length = 100)
    private String referencePaiement;

    @Column(name = "commentaire", length = 500)
    private String commentaire;

    @Column(name = "exercice")
    private Integer exercice;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public EcheanceFiscale() {}

    public Integer getExercice() { return exercice; }
    public void setExercice(Integer exercice) { this.exercice = exercice; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Societe getSociete() { return societe; }
    public void setSociete(Societe societe) { this.societe = societe; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

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

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
