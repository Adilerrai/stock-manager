package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.StatutLivraison;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CommandeDTO {
    private Long id;
    private String numeroCommande;
    private Long fournisseurId;
    private String fournisseurNom;
    private StatutCommande statut;
    private StatutLivraison statutLivraison;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC+1")
    private LocalDateTime dateCommande;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateLivraisonPrevue;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateLivraisonReelle;
    private BigDecimal montantTotal;
    private Boolean appliquerTVA;
    private BigDecimal tauxTVA;
    private BigDecimal montantHT;
    private BigDecimal montantTVA;
    private BigDecimal montantTTC;
    private String devise;
    private String observations;
    private List<LigneCommandeDTO> lignesCommande;
    private List<LivraisonDTO> livraisons = new java.util.ArrayList<>();

    // ===== Configuration import =====
    private com.gestion.persistent.enums.TypeAchat typeAchat;
    private com.gestion.persistent.enums.Incoterm incoterm;
    private String lieuIncoterm;
    private String portEmbarquement;
    private Long lieuDestinationId;
    private String lieuDestinationLibelle;
    private com.gestion.persistent.enums.ModeTransport modeTransport;
    private Long conditionPaiementId;
    private String conditionPaiementLibelle;
    private BigDecimal tauxChangeCommande;
    private BigDecimal tauxChangeDouane;

    // ===== Expédition / douane =====
    private String numeroBl;
    private String numeroConteneur;
    private String navire;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateEtd;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateEta;
    private String numeroDum;
    @JsonFormat(pattern = "yyyy-MM-dd['T'HH:mm[:ss][.SSS]]", timezone = "UTC")
    private LocalDateTime dateDum;

    // ===== Totaux coût de revient (lecture seule) =====
    private BigDecimal montantMarchandiseDevise;
    private BigDecimal montantMarchandiseMad;
    private BigDecimal valeurDouaneMad;
    private BigDecimal totalChargesEstimeesMad;
    private BigDecimal totalChargesReellesMad;
    private BigDecimal totalChargesMad;
    private BigDecimal coutRevientTotalMad;
    private BigDecimal coefficientApproche;
    private com.gestion.persistent.enums.StatutCouts statutCouts;

    // Constructors
    public CommandeDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public String getFournisseurNom() { return fournisseurNom; }
    public void setFournisseurNom(String fournisseurNom) { this.fournisseurNom = fournisseurNom; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public StatutLivraison getStatutLivraison() { return statutLivraison; }
    public void setStatutLivraison(StatutLivraison statutLivraison) { this.statutLivraison = statutLivraison; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public LocalDateTime getDateLivraisonReelle() { return dateLivraisonReelle; }
    public void setDateLivraisonReelle(LocalDateTime dateLivraisonReelle) { this.dateLivraisonReelle = dateLivraisonReelle; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public Boolean getAppliquerTVA() { return appliquerTVA; }
    public void setAppliquerTVA(Boolean appliquerTVA) { this.appliquerTVA = appliquerTVA; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getMontantTVA() { return montantTVA; }
    public void setMontantTVA(BigDecimal montantTVA) { this.montantTVA = montantTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public List<LigneCommandeDTO> getLignesCommande() { return lignesCommande; }
    public void setLignesCommande(List<LigneCommandeDTO> lignesCommande) { this.lignesCommande = lignesCommande; }

    public List<LivraisonDTO> getLivraisons() { return livraisons; }
    public void setLivraisons(List<LivraisonDTO> livraisons) { this.livraisons = livraisons != null ? livraisons : new java.util.ArrayList<>(); }

    public com.gestion.persistent.enums.TypeAchat getTypeAchat() { return typeAchat; }
    public void setTypeAchat(com.gestion.persistent.enums.TypeAchat typeAchat) { this.typeAchat = typeAchat; }

    public com.gestion.persistent.enums.Incoterm getIncoterm() { return incoterm; }
    public void setIncoterm(com.gestion.persistent.enums.Incoterm incoterm) { this.incoterm = incoterm; }

    public String getLieuIncoterm() { return lieuIncoterm; }
    public void setLieuIncoterm(String lieuIncoterm) { this.lieuIncoterm = lieuIncoterm; }

    public String getPortEmbarquement() { return portEmbarquement; }
    public void setPortEmbarquement(String portEmbarquement) { this.portEmbarquement = portEmbarquement; }

    public Long getLieuDestinationId() { return lieuDestinationId; }
    public void setLieuDestinationId(Long lieuDestinationId) { this.lieuDestinationId = lieuDestinationId; }

    public String getLieuDestinationLibelle() { return lieuDestinationLibelle; }
    public void setLieuDestinationLibelle(String lieuDestinationLibelle) { this.lieuDestinationLibelle = lieuDestinationLibelle; }

    public com.gestion.persistent.enums.ModeTransport getModeTransport() { return modeTransport; }
    public void setModeTransport(com.gestion.persistent.enums.ModeTransport modeTransport) { this.modeTransport = modeTransport; }

    public Long getConditionPaiementId() { return conditionPaiementId; }
    public void setConditionPaiementId(Long conditionPaiementId) { this.conditionPaiementId = conditionPaiementId; }

    public String getConditionPaiementLibelle() { return conditionPaiementLibelle; }
    public void setConditionPaiementLibelle(String conditionPaiementLibelle) { this.conditionPaiementLibelle = conditionPaiementLibelle; }

    public BigDecimal getTauxChangeCommande() { return tauxChangeCommande; }
    public void setTauxChangeCommande(BigDecimal tauxChangeCommande) { this.tauxChangeCommande = tauxChangeCommande; }

    public BigDecimal getTauxChangeDouane() { return tauxChangeDouane; }
    public void setTauxChangeDouane(BigDecimal tauxChangeDouane) { this.tauxChangeDouane = tauxChangeDouane; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public String getNumeroConteneur() { return numeroConteneur; }
    public void setNumeroConteneur(String numeroConteneur) { this.numeroConteneur = numeroConteneur; }

    public String getNavire() { return navire; }
    public void setNavire(String navire) { this.navire = navire; }

    public LocalDateTime getDateEtd() { return dateEtd; }
    public void setDateEtd(LocalDateTime dateEtd) { this.dateEtd = dateEtd; }

    public LocalDateTime getDateEta() { return dateEta; }
    public void setDateEta(LocalDateTime dateEta) { this.dateEta = dateEta; }

    public String getNumeroDum() { return numeroDum; }
    public void setNumeroDum(String numeroDum) { this.numeroDum = numeroDum; }

    public LocalDateTime getDateDum() { return dateDum; }
    public void setDateDum(LocalDateTime dateDum) { this.dateDum = dateDum; }

    public BigDecimal getMontantMarchandiseDevise() { return montantMarchandiseDevise; }
    public void setMontantMarchandiseDevise(BigDecimal montantMarchandiseDevise) { this.montantMarchandiseDevise = montantMarchandiseDevise; }

    public BigDecimal getMontantMarchandiseMad() { return montantMarchandiseMad; }
    public void setMontantMarchandiseMad(BigDecimal montantMarchandiseMad) { this.montantMarchandiseMad = montantMarchandiseMad; }

    public BigDecimal getValeurDouaneMad() { return valeurDouaneMad; }
    public void setValeurDouaneMad(BigDecimal valeurDouaneMad) { this.valeurDouaneMad = valeurDouaneMad; }

    public BigDecimal getTotalChargesEstimeesMad() { return totalChargesEstimeesMad; }
    public void setTotalChargesEstimeesMad(BigDecimal totalChargesEstimeesMad) { this.totalChargesEstimeesMad = totalChargesEstimeesMad; }

    public BigDecimal getTotalChargesReellesMad() { return totalChargesReellesMad; }
    public void setTotalChargesReellesMad(BigDecimal totalChargesReellesMad) { this.totalChargesReellesMad = totalChargesReellesMad; }

    public BigDecimal getTotalChargesMad() { return totalChargesMad; }
    public void setTotalChargesMad(BigDecimal totalChargesMad) { this.totalChargesMad = totalChargesMad; }

    public BigDecimal getCoutRevientTotalMad() { return coutRevientTotalMad; }
    public void setCoutRevientTotalMad(BigDecimal coutRevientTotalMad) { this.coutRevientTotalMad = coutRevientTotalMad; }

    public BigDecimal getCoefficientApproche() { return coefficientApproche; }
    public void setCoefficientApproche(BigDecimal coefficientApproche) { this.coefficientApproche = coefficientApproche; }

    public com.gestion.persistent.enums.StatutCouts getStatutCouts() { return statutCouts; }
    public void setStatutCouts(com.gestion.persistent.enums.StatutCouts statutCouts) { this.statutCouts = statutCouts; }
}
