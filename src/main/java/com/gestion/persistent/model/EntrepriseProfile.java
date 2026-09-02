package com.gestion.persistent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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
    private String devise = "DZD";

    @Column(name = "date_mise_a_jour")
    private LocalDateTime dateMiseAJour = LocalDateTime.now();

    public EntrepriseProfile() {}

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

    public byte[] getLogoData() { return logoData; }
    public void setLogoData(byte[] logoData) { this.logoData = logoData; }

    public String getLogoContentType() { return logoContentType; }
    public void setLogoContentType(String logoContentType) { this.logoContentType = logoContentType; }

    public String getLogoFileName() { return logoFileName; }
    public void setLogoFileName(String logoFileName) { this.logoFileName = logoFileName; }

    public String getPiedPage() { return piedPage; }
    public void setPiedPage(String piedPage) { this.piedPage = piedPage; }

    public String getDevise() { return devise; }
    public void setDevise(String devise) { this.devise = devise; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }

    public boolean hasLogo() {
        return logoData != null && logoData.length > 0;
    }
}
