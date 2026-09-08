package com.acommon.persistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EntrepriseRegistrationRequest {

    // === INFORMATIONS DE L'ENTREPRISE (TENANT) ===
    @NotBlank(message = "Le nom de l'entreprise est obligatoire")
    private String nomEntreprise;

    private Long tenantId; // Optionnel : si spécifié par le SuperAdmin

    private String activite;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String email;
    private String siteWeb;
    private String devise = "MAD";

    // Informations légales & fiscales
    private String registreCommerce;
    private String numeroIdentificationFiscale;
    private String numeroIdentificationStatistique;
    private String articleImposition;
    private String compteBancaireRib;
    private String nomBanque;

    // === COMPTE ADMINISTRATEUR INITIAL ===
    @NotBlank(message = "L'email de l'administrateur est obligatoire")
    @Email(message = "Format d'email invalide")
    private String adminEmail;

    @NotBlank(message = "Le nom d'utilisateur administrateur est obligatoire")
    private String adminUsername;

    @NotBlank(message = "Le mot de passe administrateur est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String adminPassword;

    @NotBlank(message = "Le nom complet de l'administrateur est obligatoire")
    private String adminNomComplet;

    private String adminTelephone;

    public EntrepriseRegistrationRequest() {
    }

    // Getters and Setters

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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

    public String getDevise() {
        return devise;
    }

    public void setDevise(String devise) {
        this.devise = devise;
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

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminNomComplet() {
        return adminNomComplet;
    }

    public void setAdminNomComplet(String adminNomComplet) {
        this.adminNomComplet = adminNomComplet;
    }

    public String getAdminTelephone() {
        return adminTelephone;
    }

    public void setAdminTelephone(String adminTelephone) {
        this.adminTelephone = adminTelephone;
    }
}
