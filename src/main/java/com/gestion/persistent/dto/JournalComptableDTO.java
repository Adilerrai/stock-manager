package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gestion.persistent.enums.TypeJournal;

import java.math.BigDecimal;
import java.time.LocalDate;

public class JournalComptableDTO {
    private Long id;
    private String code;
    private String libelle;
    private TypeJournal typeJournal;
    private Boolean actif = true;

    // Métriques agrégées pour affichage PCGM sans requêtes N+1
    private Long nombreEcritures = 0L;
    private BigDecimal totalDebit = BigDecimal.ZERO;
    private BigDecimal totalCredit = BigDecimal.ZERO;
    private LocalDate derniereEcritureDate;

    public JournalComptableDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public TypeJournal getTypeJournal() { return typeJournal; }
    public void setTypeJournal(TypeJournal typeJournal) { this.typeJournal = typeJournal; }

    // Rétrocompatibilité frontend : expose également 'type'
    @JsonProperty("type")
    public String getTypeString() {
        return typeJournal != null ? typeJournal.name() : null;
    }

    public String getTypeJournalLibelle() {
        return typeJournal != null ? typeJournal.getLibelle() : null;
    }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public Long getNombreEcritures() { return nombreEcritures; }
    public void setNombreEcritures(Long nombreEcritures) { this.nombreEcritures = nombreEcritures != null ? nombreEcritures : 0L; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO; }

    public LocalDate getDerniereEcritureDate() { return derniereEcritureDate; }
    public void setDerniereEcritureDate(LocalDate derniereEcritureDate) { this.derniereEcritureDate = derniereEcritureDate; }
}
