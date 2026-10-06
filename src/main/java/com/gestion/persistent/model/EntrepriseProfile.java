package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entreprise_profiles")
public class EntrepriseProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "point_de_vente_id", unique = true, nullable = false)
    private Long pointDeVenteId;

    @Column(name = "nom_entreprise", nullable = false)
    private String nomEntreprise;

    private String activite;

    private String adresse;

    private String ville;

    @Column(name = "code_postal")
    private String codePostal;

    private String telephone;

    @Column(name = "telephone_secondaire")
    private String telephoneSecondaire;

    private String email;

    @Column(name = "site_web")
    private String siteWeb;

    @Column(name = "registre_commerce")
    private String registreCommerce; // RC

    @Column(name = "numero_identification_fiscale")
    private String numeroIdentificationFiscale; // NIF

    @Column(name = "numero_identification_statistique")
    private String numeroIdentificationStatistique; // NIS

    @Column(name = "article_imposition")
    private String articleImposition; // AI

    @Column(name = "compte_bancaire_rib")
    private String compteBancaireRib; // RIB

    @Column(name = "nom_banque")
    private String nomBanque;

    @Column(name = "patente")
    private String patente; // Patente (Maroc)

    @Column(name = "ice")
    private String ice; // Identifiant Commun de l'Entreprise (Maroc)

    @Column(name = "cnss")
    private String cnss; // CNSS (Maroc)

    @Column(name = "gsm")
    private String gsm; // GSM / Mobile

    @JsonIgnore
    @Column(name = "logo_data", columnDefinition = "bytea")
    private byte[] logoData;

    @Column(name = "logo_content_type")
    private String logoContentType;

    @Column(name = "logo_file_name")
    private String logoFileName;

    @Column(name = "pied_page", length = 1000)
    private String piedPage;

    @Column(name = "devise")
    private String devise = "MAD";

    @Column(name = "vente_stock_negatif")
    private Boolean venteStockNegatif = false;

    // === 5 RÈGLES MÉTIER ERP CONFIGURABLES ===

    // Règle 1 : Approbation d'achat par seuil de montant
    @Column(name = "approbation_achat_active")
    private Boolean approbationAchatActive = false;

    @Column(name = "seuil_approbation_achat", precision = 12, scale = 2)
    private BigDecimal seuilApprobationAchat = new BigDecimal("10000.00");

    // Règle 2 : Blocage dépassement plafond de crédit / encours client
    @Column(name = "blocage_encours_client_actif")
    private Boolean blocageEncoursClientActif = false;

    // Règle 3 : Validation des remises commerciales exceptionnelles
    @Column(name = "validation_remise_max_active")
    private Boolean validationRemiseMaxActive = false;

    @Column(name = "seuil_remise_max_pourcentage", precision = 5, scale = 2)
    private BigDecimal seuilRemiseMaxPourcentage = new BigDecimal("10.00");

    // Règle 4 : Séparation des tâches / interdiction auto-approbation
    @Column(name = "interdiction_auto_approbation")
    private Boolean interdictionAutoApprobation = false;

    // Règle 5 : Rapprochement à 3 voies / tolérance d'écart de réception
    @Column(name = "tolerance_ecart_reception_active")
    private Boolean toleranceEcartReceptionActive = false;

    @Column(name = "tolerance_ecart_pourcentage", precision = 5, scale = 2)
    private BigDecimal toleranceEcartPourcentage = new BigDecimal("5.00");

    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour = LocalDateTime.now();

    public EntrepriseProfile() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPointDeVenteId() {
        return pointDeVenteId;
    }

    public void setPointDeVenteId(Long pointDeVenteId) {
        this.pointDeVenteId = pointDeVenteId;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getActivite() {
        return activite;
    }

    public void setActivite(String activite) {
        this.activite = activite;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getTelephoneSecondaire() {
        return telephoneSecondaire;
    }

    public void setTelephoneSecondaire(String telephoneSecondaire) {
        this.telephoneSecondaire = telephoneSecondaire;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getRegistreCommerce() {
        return registreCommerce;
    }

    public void setRegistreCommerce(String registreCommerce) {
        this.registreCommerce = registreCommerce;
    }

    public String getNumeroIdentificationFiscale() {
        return numeroIdentificationFiscale;
    }

    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) {
        this.numeroIdentificationFiscale = numeroIdentificationFiscale;
    }

    public String getNumeroIdentificationStatistique() {
        return numeroIdentificationStatistique;
    }

    public void setNumeroIdentificationStatistique(String numeroIdentificationStatistique) {
        this.numeroIdentificationStatistique = numeroIdentificationStatistique;
    }

    public String getArticleImposition() {
        return articleImposition;
    }

    public void setArticleImposition(String articleImposition) {
        this.articleImposition = articleImposition;
    }

    public String getCompteBancaireRib() {
        return compteBancaireRib;
    }

    public void setCompteBancaireRib(String compteBancaireRib) {
        this.compteBancaireRib = compteBancaireRib;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public byte[] getLogoData() {
        return logoData;
    }

    public void setLogoData(byte[] logoData) {
        this.logoData = logoData;
    }

    public String getLogoContentType() {
        return logoContentType;
    }

    public void setLogoContentType(String logoContentType) {
        this.logoContentType = logoContentType;
    }

    public String getLogoFileName() {
        return logoFileName;
    }

    public void setLogoFileName(String logoFileName) {
        this.logoFileName = logoFileName;
    }

    public String getPiedPage() {
        return piedPage;
    }

    public void setPiedPage(String piedPage) {
        this.piedPage = piedPage;
    }

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
    }

    public Boolean getVenteStockNegatif() {
        return venteStockNegatif != null && venteStockNegatif;
    }

    public void setVenteStockNegatif(Boolean venteStockNegatif) {
        this.venteStockNegatif = venteStockNegatif;
    }

    public LocalDateTime getDateMiseAJour() {
        return dateMiseAJour;
    }

    public void setDateMiseAJour(LocalDateTime dateMiseAJour) {
        this.dateMiseAJour = dateMiseAJour;
    }

    public boolean hasLogo() {
        return logoData != null && logoData.length > 0;
    }

    public String getPatente() {
        return patente != null ? patente : articleImposition;
    }

    public void setPatente(String patente) {
        this.patente = patente;
        if (this.articleImposition == null) {
            this.articleImposition = patente;
        }
    }

    public String getIce() {
        return ice != null ? ice : numeroIdentificationStatistique;
    }

    public void setIce(String ice) {
        this.ice = ice;
        if (this.numeroIdentificationStatistique == null) {
            this.numeroIdentificationStatistique = ice;
        }
    }

    public String getCnss() {
        return cnss;
    }

    public void setCnss(String cnss) {
        this.cnss = cnss;
    }

    public String getGsm() {
        return gsm != null ? gsm : telephoneSecondaire;
    }

    public void setGsm(String gsm) {
        this.gsm = gsm;
        if (this.telephoneSecondaire == null) {
            this.telephoneSecondaire = gsm;
        }
    }

    // === Getters & Setters des 5 Règles Métier ===

    public Boolean getApprobationAchatActive() {
        return approbationAchatActive != null && approbationAchatActive;
    }

    public void setApprobationAchatActive(Boolean approbationAchatActive) {
        this.approbationAchatActive = approbationAchatActive;
    }

    public BigDecimal getSeuilApprobationAchat() {
        return seuilApprobationAchat != null ? seuilApprobationAchat : new BigDecimal("10000.00");
    }

    public void setSeuilApprobationAchat(BigDecimal seuilApprobationAchat) {
        this.seuilApprobationAchat = seuilApprobationAchat;
    }

    public Boolean getBlocageEncoursClientActif() {
        return blocageEncoursClientActif != null && blocageEncoursClientActif;
    }

    public void setBlocageEncoursClientActif(Boolean blocageEncoursClientActif) {
        this.blocageEncoursClientActif = blocageEncoursClientActif;
    }

    public Boolean getValidationRemiseMaxActive() {
        return validationRemiseMaxActive != null && validationRemiseMaxActive;
    }

    public void setValidationRemiseMaxActive(Boolean validationRemiseMaxActive) {
        this.validationRemiseMaxActive = validationRemiseMaxActive;
    }

    public BigDecimal getSeuilRemiseMaxPourcentage() {
        return seuilRemiseMaxPourcentage != null ? seuilRemiseMaxPourcentage : new BigDecimal("10.00");
    }

    public void setSeuilRemiseMaxPourcentage(BigDecimal seuilRemiseMaxPourcentage) {
        this.seuilRemiseMaxPourcentage = seuilRemiseMaxPourcentage;
    }

    public Boolean getInterdictionAutoApprobation() {
        return interdictionAutoApprobation != null && interdictionAutoApprobation;
    }

    public void setInterdictionAutoApprobation(Boolean interdictionAutoApprobation) {
        this.interdictionAutoApprobation = interdictionAutoApprobation;
    }

    public Boolean getToleranceEcartReceptionActive() {
        return toleranceEcartReceptionActive != null && toleranceEcartReceptionActive;
    }

    public void setToleranceEcartReceptionActive(Boolean toleranceEcartReceptionActive) {
        this.toleranceEcartReceptionActive = toleranceEcartReceptionActive;
    }

    public BigDecimal getToleranceEcartPourcentage() {
        return toleranceEcartPourcentage != null ? toleranceEcartPourcentage : new BigDecimal("5.00");
    }

    public void setToleranceEcartPourcentage(BigDecimal toleranceEcartPourcentage) {
        this.toleranceEcartPourcentage = toleranceEcartPourcentage;
    }
}
