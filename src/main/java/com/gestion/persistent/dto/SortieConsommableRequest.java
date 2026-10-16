package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SortieConsommableRequest {

    private BigDecimal quantite;
    private String beneficiaire;  // Ex: "Service RH", "Atelier", "Directeur"
    private String motif;         // Ex: "Réunion générale", "Dotation nouvelle recrue"
    private LocalDate dateSortie; // Date de consommation

    public SortieConsommableRequest() {}

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDate getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(LocalDate dateSortie) {
        this.dateSortie = dateSortie;
    }
}
