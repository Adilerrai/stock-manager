package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PieceCommercialeEnAttenteDTO {

    private String typePiece; // "FACTURE_VENTE", "FACTURE_ACHAT", "ENCAISSEMENT_CLIENT", "REGLEMENT_FOURNISSEUR"
    private Long id;
    private String reference;
    private LocalDate datePiece;
    private String tiersNom;
    private BigDecimal montantHt = BigDecimal.ZERO;
    private BigDecimal montantTva = BigDecimal.ZERO;
    private BigDecimal montantTtc = BigDecimal.ZERO;
    private String journalCible; // "VE", "AC", "CA", "BQ"
    private boolean deversee = false;

    public PieceCommercialeEnAttenteDTO() {}

    public PieceCommercialeEnAttenteDTO(String typePiece, Long id, String reference, LocalDate datePiece,
                                        String tiersNom, BigDecimal montantHt, BigDecimal montantTva,
                                        BigDecimal montantTtc, String journalCible, boolean deversee) {
        this.typePiece = typePiece;
        this.id = id;
        this.reference = reference;
        this.datePiece = datePiece;
        this.tiersNom = tiersNom;
        this.montantHt = montantHt != null ? montantHt : BigDecimal.ZERO;
        this.montantTva = montantTva != null ? montantTva : BigDecimal.ZERO;
        this.montantTtc = montantTtc != null ? montantTtc : BigDecimal.ZERO;
        this.journalCible = journalCible;
        this.deversee = deversee;
    }

    public String getTypePiece() { return typePiece; }
    public void setTypePiece(String typePiece) { this.typePiece = typePiece; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public LocalDate getDatePiece() { return datePiece; }
    public void setDatePiece(LocalDate datePiece) { this.datePiece = datePiece; }

    public String getTiersNom() { return tiersNom; }
    public void setTiersNom(String tiersNom) { this.tiersNom = tiersNom; }

    public BigDecimal getMontantHt() { return montantHt; }
    public void setMontantHt(BigDecimal montantHt) { this.montantHt = montantHt; }

    public BigDecimal getMontantTva() { return montantTva; }
    public void setMontantTva(BigDecimal montantTva) { this.montantTva = montantTva; }

    public BigDecimal getMontantTtc() { return montantTtc; }
    public void setMontantTtc(BigDecimal montantTtc) { this.montantTtc = montantTtc; }

    public String getJournalCible() { return journalCible; }
    public void setJournalCible(String journalCible) { this.journalCible = journalCible; }

    public boolean isDeversee() { return deversee; }
    public void setDeversee(boolean deversee) { this.deversee = deversee; }
}
