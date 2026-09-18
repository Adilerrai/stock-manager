package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RegularisationResultDTO {

    private Long idEcritureInventaire;
    private String numeroPieceInventaire;
    private LocalDate dateInventaire;
    private Long idEcritureExtourne;
    private String numeroPieceExtourne;
    private LocalDate dateExtourne;
    private BigDecimal montant;
    private String type;
    private String message;

    public RegularisationResultDTO() {}

    public Long getIdEcritureInventaire() { return idEcritureInventaire; }
    public void setIdEcritureInventaire(Long idEcritureInventaire) { this.idEcritureInventaire = idEcritureInventaire; }

    public String getNumeroPieceInventaire() { return numeroPieceInventaire; }
    public void setNumeroPieceInventaire(String numeroPieceInventaire) { this.numeroPieceInventaire = numeroPieceInventaire; }

    public LocalDate getDateInventaire() { return dateInventaire; }
    public void setDateInventaire(LocalDate dateInventaire) { this.dateInventaire = dateInventaire; }

    public Long getIdEcritureExtourne() { return idEcritureExtourne; }
    public void setIdEcritureExtourne(Long idEcritureExtourne) { this.idEcritureExtourne = idEcritureExtourne; }

    public String getNumeroPieceExtourne() { return numeroPieceExtourne; }
    public void setNumeroPieceExtourne(String numeroPieceExtourne) { this.numeroPieceExtourne = numeroPieceExtourne; }

    public LocalDate getDateExtourne() { return dateExtourne; }
    public void setDateExtourne(LocalDate dateExtourne) { this.dateExtourne = dateExtourne; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
