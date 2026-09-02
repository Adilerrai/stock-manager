package com.gestion.persistent.dto;

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
    private String compteBancaireRib;
    private String nomBanque;
    private boolean hasLogo;
    private String logoFileName;
    private String piedPage;
    private String devise;
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

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }
}
