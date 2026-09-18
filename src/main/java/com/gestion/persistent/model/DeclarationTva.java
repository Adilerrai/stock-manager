package com.gestion.persistent.model;

import com.gestion.persistent.enums.RegimeTva;
import com.gestion.persistent.enums.StatutDeclarationTva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "declarations_tva")
public class DeclarationTva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String periode; // Ex: "2026-09" ou "2026-T3"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegimeTva regime = RegimeTva.ENCAISSEMENT;

    @Column(name = "tva_collectee", precision = 15, scale = 2)
    private BigDecimal tvaCollectee = BigDecimal.ZERO;

    @Column(name = "tva_deductible_charges", precision = 15, scale = 2)
    private BigDecimal tvaDeductibleCharges = BigDecimal.ZERO;

    @Column(name = "tva_deductible_immo", precision = 15, scale = 2)
    private BigDecimal tvaDeductibleImmo = BigDecimal.ZERO;

    @Column(name = "tva_deductible_total", precision = 15, scale = 2)
    private BigDecimal tvaDeductibleTotal = BigDecimal.ZERO;

    @Column(name = "credit_tva_anterieur", precision = 15, scale = 2)
    private BigDecimal creditTvaAnterieur = BigDecimal.ZERO;

    @Column(name = "tva_a_payer", precision = 15, scale = 2)
    private BigDecimal tvaAPayer = BigDecimal.ZERO;

    @Column(name = "credit_tva_reportable", precision = 15, scale = 2)
    private BigDecimal creditTvaReportable = BigDecimal.ZERO;

    @Column(name = "prorata", precision = 5, scale = 2)
    private BigDecimal prorata = new BigDecimal("100.00"); // 100% par défaut

    @Column(name = "total_ventes_ht", precision = 15, scale = 2)
    private BigDecimal totalVentesHT = BigDecimal.ZERO;

    @Column(name = "total_achats_ht", precision = 15, scale = 2)
    private BigDecimal totalAchatsHT = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutDeclarationTva statut = StatutDeclarationTva.BROUILLON;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "validee_par")
    private String valideePar;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "point_de_vente_id", nullable = false)
    private Long pointDeVenteId;

    public DeclarationTva() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPeriode() { return periode; }
    public void setPeriode(String periode) { this.periode = periode; }

    public RegimeTva getRegime() { return regime; }
    public void setRegime(RegimeTva regime) { this.regime = regime; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee; }

    public BigDecimal getTvaDeductibleCharges() { return tvaDeductibleCharges; }
    public void setTvaDeductibleCharges(BigDecimal tvaDeductibleCharges) { this.tvaDeductibleCharges = tvaDeductibleCharges; }

    public BigDecimal getTvaDeductibleImmo() { return tvaDeductibleImmo; }
    public void setTvaDeductibleImmo(BigDecimal tvaDeductibleImmo) { this.tvaDeductibleImmo = tvaDeductibleImmo; }

    public BigDecimal getTvaDeductibleTotal() { return tvaDeductibleTotal; }
    public void setTvaDeductibleTotal(BigDecimal tvaDeductibleTotal) { this.tvaDeductibleTotal = tvaDeductibleTotal; }

    public BigDecimal getCreditTvaAnterieur() { return creditTvaAnterieur; }
    public void setCreditTvaAnterieur(BigDecimal creditTvaAnterieur) { this.creditTvaAnterieur = creditTvaAnterieur; }

    public BigDecimal getTvaAPayer() { return tvaAPayer; }
    public void setTvaAPayer(BigDecimal tvaAPayer) { this.tvaAPayer = tvaAPayer; }

    public BigDecimal getCreditTvaReportable() { return creditTvaReportable; }
    public void setCreditTvaReportable(BigDecimal creditTvaReportable) { this.creditTvaReportable = creditTvaReportable; }

    public BigDecimal getProrata() { return prorata; }
    public void setProrata(BigDecimal prorata) { this.prorata = prorata; }

    public BigDecimal getTotalVentesHT() { return totalVentesHT; }
    public void setTotalVentesHT(BigDecimal totalVentesHT) { this.totalVentesHT = totalVentesHT; }

    public BigDecimal getTotalAchatsHT() { return totalAchatsHT; }
    public void setTotalAchatsHT(BigDecimal totalAchatsHT) { this.totalAchatsHT = totalAchatsHT; }

    public StatutDeclarationTva getStatut() { return statut; }
    public void setStatut(StatutDeclarationTva statut) { this.statut = statut; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateValidation() { return dateValidation; }
    public void setDateValidation(LocalDateTime dateValidation) { this.dateValidation = dateValidation; }

    public String getValideePar() { return valideePar; }
    public void setValideePar(String valideePar) { this.valideePar = valideePar; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
