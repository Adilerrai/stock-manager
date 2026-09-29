package com.gestion.persistent.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaisieKilometriqueDTO {

    private String journalCode; // "AC", "VE", "OD", "BQ", "CA"
    private Long journalId;
    private LocalDate dateEcriture;
    private String numeroPiece;
    private String referencePiece;
    private String libellePiece;
    private Boolean validee = true;
    private boolean autoEquilibrer = false; // si true, équilibre automatiquement la pièce sur le compte de contrepartie
    private String compteContrepartieAuto; // ex: "44110000" pour achat, "34210000" pour vente, "51410000" pour banque

    private List<LigneSaisieKilometriqueDTO> lignes = new ArrayList<>();

    public SaisieKilometriqueDTO() {}

    public String getJournalCode() { return journalCode; }
    public void setJournalCode(String journalCode) { this.journalCode = journalCode; }

    public Long getJournalId() { return journalId; }
    public void setJournalId(Long journalId) { this.journalId = journalId; }

    public LocalDate getDateEcriture() { return dateEcriture; }
    public void setDateEcriture(LocalDate dateEcriture) { this.dateEcriture = dateEcriture; }

    public String getNumeroPiece() { return numeroPiece; }
    public void setNumeroPiece(String numeroPiece) { this.numeroPiece = numeroPiece; }

    public String getReferencePiece() { return referencePiece; }
    public void setReferencePiece(String referencePiece) { this.referencePiece = referencePiece; }

    public String getLibellePiece() { return libellePiece; }
    public void setLibellePiece(String libellePiece) { this.libellePiece = libellePiece; }

    public Boolean getValidee() { return validee; }
    public void setValidee(Boolean validee) { this.validee = validee; }

    public boolean isAutoEquilibrer() { return autoEquilibrer; }
    public void setAutoEquilibrer(boolean autoEquilibrer) { this.autoEquilibrer = autoEquilibrer; }

    public String getCompteContrepartieAuto() { return compteContrepartieAuto; }
    public void setCompteContrepartieAuto(String compteContrepartieAuto) { this.compteContrepartieAuto = compteContrepartieAuto; }

    public List<LigneSaisieKilometriqueDTO> getLignes() { return lignes; }
    public void setLignes(List<LigneSaisieKilometriqueDTO> lignes) { this.lignes = lignes; }
}
