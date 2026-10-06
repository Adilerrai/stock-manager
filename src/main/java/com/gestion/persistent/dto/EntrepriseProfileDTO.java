package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntrepriseProfileDTO {
    private Long id;
    private Long pointDeVenteId;
    private String nomEntreprise;
    private String activite;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String telephoneSecondaire;
    private String email;
    private String siteWeb;
    private String registreCommerce;
    private String numeroIdentificationFiscale;
    private String numeroIdentificationStatistique;
    private String articleImposition;
    private String patente;
    private String ice;
    private String cnss;
    private String gsm;
    private String compteBancaireRib;
    private String nomBanque;
    private boolean hasLogo;
    private String logoFileName;
    private String piedPage;
    private String devise;
    private Boolean venteStockNegatif = false;

    // === 5 RÈGLES MÉTIER ERP CONFIGURABLES ===
    private Boolean approbationAchatActive = false;
    private BigDecimal seuilApprobationAchat = new BigDecimal("10000.00");
    private Boolean blocageEncoursClientActif = false;
    private Boolean validationRemiseMaxActive = false;
    private BigDecimal seuilRemiseMaxPourcentage = new BigDecimal("10.00");
    private Boolean interdictionAutoApprobation = false;
    private Boolean toleranceEcartReceptionActive = false;
    private BigDecimal toleranceEcartPourcentage = new BigDecimal("5.00");

    private LocalDateTime dateMiseAJour;

    public EntrepriseProfileDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }

    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getTelephoneSecondaire() { return telephoneSecondaire; }
    public void setTelephoneSecondaire(String telephoneSecondaire) { this.telephoneSecondaire = telephoneSecondaire; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getRegistreCommerce() { return registreCommerce; }
    public void setRegistreCommerce(String registreCommerce) { this.registreCommerce = registreCommerce; }

    public String getNumeroIdentificationFiscale() { return numeroIdentificationFiscale; }
    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) { this.numeroIdentificationFiscale = numeroIdentificationFiscale; }

    public String getNumeroIdentificationStatistique() { return numeroIdentificationStatistique; }
    public void setNumeroIdentificationStatistique(String numeroIdentificationStatistique) { this.numeroIdentificationStatistique = numeroIdentificationStatistique; }

    public String getArticleImposition() { return articleImposition; }
    public void setArticleImposition(String articleImposition) { this.articleImposition = articleImposition; }

    public String getCompteBancaireRib() { return compteBancaireRib; }
    public void setCompteBancaireRib(String compteBancaireRib) { this.compteBancaireRib = compteBancaireRib; }

    public String getNomBanque() { return nomBanque; }
    public void setNomBanque(String nomBanque) { this.nomBanque = nomBanque; }

    public boolean isHasLogo() { return hasLogo; }
    public void setHasLogo(boolean hasLogo) { this.hasLogo = hasLogo; }

    public String getLogoFileName() { return logoFileName; }
    public void setLogoFileName(String logoFileName) { this.logoFileName = logoFileName; }

    public String getPiedPage() { return piedPage; }
    public void setPiedPage(String piedPage) { this.piedPage = piedPage; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public Boolean getVenteStockNegatif() { return venteStockNegatif != null && venteStockNegatif; }
    public void setVenteStockNegatif(Boolean venteStockNegatif) { this.venteStockNegatif = venteStockNegatif; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }

    public String getPatente() { return patente != null ? patente : articleImposition; }
    public void setPatente(String patente) { 
        this.patente = patente; 
        if (this.articleImposition == null) this.articleImposition = patente;
    }

    public String getIce() { return ice != null ? ice : numeroIdentificationStatistique; }
    public void setIce(String ice) { 
        this.ice = ice; 
        if (this.numeroIdentificationStatistique == null) this.numeroIdentificationStatistique = ice;
    }

    public String getCnss() { return cnss; }
    public void setCnss(String cnss) { this.cnss = cnss; }

    public String getGsm() { return gsm != null ? gsm : telephoneSecondaire; }
    public void setGsm(String gsm) { 
        this.gsm = gsm; 
        if (this.telephoneSecondaire == null) this.telephoneSecondaire = gsm;
    }

    // === Getters & Setters des 5 Règles Métier ===
    public Boolean getApprobationAchatActive() { return approbationAchatActive != null && approbationAchatActive; }
    public void setApprobationAchatActive(Boolean approbationAchatActive) { this.approbationAchatActive = approbationAchatActive; }

    public BigDecimal getSeuilApprobationAchat() { return seuilApprobationAchat != null ? seuilApprobationAchat : new BigDecimal("10000.00"); }
    public void setSeuilApprobationAchat(BigDecimal seuilApprobationAchat) { this.seuilApprobationAchat = seuilApprobationAchat; }

    public Boolean getBlocageEncoursClientActif() { return blocageEncoursClientActif != null && blocageEncoursClientActif; }
    public void setBlocageEncoursClientActif(Boolean blocageEncoursClientActif) { this.blocageEncoursClientActif = blocageEncoursClientActif; }

    public Boolean getValidationRemiseMaxActive() { return validationRemiseMaxActive != null && validationRemiseMaxActive; }
    public void setValidationRemiseMaxActive(Boolean validationRemiseMaxActive) { this.validationRemiseMaxActive = validationRemiseMaxActive; }

    public BigDecimal getSeuilRemiseMaxPourcentage() { return seuilRemiseMaxPourcentage != null ? seuilRemiseMaxPourcentage : new BigDecimal("10.00"); }
    public void setSeuilRemiseMaxPourcentage(BigDecimal seuilRemiseMaxPourcentage) { this.seuilRemiseMaxPourcentage = seuilRemiseMaxPourcentage; }

    public Boolean getInterdictionAutoApprobation() { return interdictionAutoApprobation != null && interdictionAutoApprobation; }
    public void setInterdictionAutoApprobation(Boolean interdictionAutoApprobation) { this.interdictionAutoApprobation = interdictionAutoApprobation; }

    public Boolean getToleranceEcartReceptionActive() { return toleranceEcartReceptionActive != null && toleranceEcartReceptionActive; }
    public void setToleranceEcartReceptionActive(Boolean toleranceEcartReceptionActive) { this.toleranceEcartReceptionActive = toleranceEcartReceptionActive; }

    public BigDecimal getToleranceEcartPourcentage() { return toleranceEcartPourcentage != null ? toleranceEcartPourcentage : new BigDecimal("5.00"); }
    public void setToleranceEcartPourcentage(BigDecimal toleranceEcartPourcentage) { this.toleranceEcartPourcentage = toleranceEcartPourcentage; }
}
