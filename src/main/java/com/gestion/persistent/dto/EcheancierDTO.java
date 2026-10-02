package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EcheancierDTO {
    private BigDecimal totalAEncaisser = BigDecimal.ZERO;
    private BigDecimal totalAPayer = BigDecimal.ZERO;
    private BigDecimal soldePrevisionnel = BigDecimal.ZERO;
    private List<LigneEcheanceDTO> echeances = new ArrayList<>();

    public static class LigneEcheanceDTO {
        private LocalDate dateEcheance;
        private String sens; // ENCAISSEMENT (Client) ou DECAISSEMENT (Fournisseur)
        private String tiersNom;
        private String typeDocument; // FACTURE_VENTE, FACTURE_ACHAT, CHEQUE
        private String reference;
        private BigDecimal montant;
        private String statut;
        private Long documentId;
        private String banque;

        public LigneEcheanceDTO() {}

        public LigneEcheanceDTO(LocalDate dateEcheance, String sens, String tiersNom,
                                String typeDocument, String reference, BigDecimal montant, String statut) {
            this(dateEcheance, sens, tiersNom, typeDocument, reference, montant, statut, null, null);
        }

        public LigneEcheanceDTO(LocalDate dateEcheance, String sens, String tiersNom,
                                String typeDocument, String reference, BigDecimal montant,
                                String statut, Long documentId, String banque) {
            this.dateEcheance = dateEcheance;
            this.sens = sens;
            this.tiersNom = tiersNom;
            this.typeDocument = typeDocument;
            this.reference = reference;
            this.montant = montant;
            this.statut = statut;
            this.documentId = documentId;
            this.banque = banque;
        }

        public LocalDate getDateEcheance() { return dateEcheance; }
        public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }

        public String getSens() { return sens; }
        public void setSens(String sens) { this.sens = sens; }

        public String getTiersNom() { return tiersNom; }
        public void setTiersNom(String tiersNom) { this.tiersNom = tiersNom; }

        public String getTypeDocument() { return typeDocument; }
        public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }

        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }

        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }

        public String getBanque() { return banque; }
        public void setBanque(String banque) { this.banque = banque; }
    }

    public EcheancierDTO() {}

    public BigDecimal getTotalAEncaisser() { return totalAEncaisser; }
    public void setTotalAEncaisser(BigDecimal totalAEncaisser) { this.totalAEncaisser = totalAEncaisser; }

    public BigDecimal getTotalAPayer() { return totalAPayer; }
    public void setTotalAPayer(BigDecimal totalAPayer) { this.totalAPayer = totalAPayer; }

    public BigDecimal getSoldePrevisionnel() { return soldePrevisionnel; }
    public void setSoldePrevisionnel(BigDecimal soldePrevisionnel) { this.soldePrevisionnel = soldePrevisionnel; }

    public List<LigneEcheanceDTO> getEcheances() { return echeances; }
    public void setEcheances(List<LigneEcheanceDTO> echeances) { this.echeances = echeances; }
}
