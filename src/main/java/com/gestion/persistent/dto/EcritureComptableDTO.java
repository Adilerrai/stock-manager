package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EcritureComptableDTO {
    private Long id;
    private String numeroPiece;
    private LocalDate dateEcriture;
    private String libelle;
    private String referencePiece;
    private Boolean validee;
    private Long journalId;
    private String journalCode;
    private String journalLibelle;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private List<LigneEcritureDTO> lignes = new ArrayList<>();

    public EcritureComptableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroPiece() { return numeroPiece; }
    public void setNumeroPiece(String numeroPiece) { this.numeroPiece = numeroPiece; }

    public LocalDate getDateEcriture() { return dateEcriture; }
    public void setDateEcriture(LocalDate dateEcriture) { this.dateEcriture = dateEcriture; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getReferencePiece() { return referencePiece; }
    public void setReferencePiece(String referencePiece) { this.referencePiece = referencePiece; }

    public Boolean getValidee() { return validee; }
    public void setValidee(Boolean validee) { this.validee = validee; }

    public Long getJournalId() { return journalId; }
    public void setJournalId(Long journalId) { this.journalId = journalId; }

    public String getJournalCode() { return journalCode; }
    public void setJournalCode(String journalCode) { this.journalCode = journalCode; }

    public String getJournalLibelle() { return journalLibelle; }
    public void setJournalLibelle(String journalLibelle) { this.journalLibelle = journalLibelle; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public List<LigneEcritureDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneEcritureDTO> lignes) { this.lignes = lignes; }
}
