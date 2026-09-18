package com.gestion.persistent.model;

import com.gestion.persistent.enums.RegimeTva;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "societes")
public class Societe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mere_id")
    private Long mereId; // Le cabinet comptable / tenant Mère propriétaire

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId; // Le cabinet comptable / tenant propriétaire (synonyme de mereId)

    @Column(name = "code", nullable = false, length = 50)
    private String code; // Ex: "SOC01", "ALPHA"

    @Column(name = "raison_sociale", nullable = false, length = 200)
    private String raisonSociale;

    @Column(name = "forme_juridique", length = 50)
    private String formeJuridique = "SARL"; // SARL, SARL_AU, SA, SAS, SNC, PERSONNE_PHYSIQUE, AUTO_ENTREPRENEUR

    @Column(name = "ice", length = 30)
    private String ice; // 15 chiffres

    @Column(name = "rc", length = 50)
    private String rc; // Registre de commerce

    @Column(name = "identifiant_fiscal", length = 50)
    private String identifiantFiscal; // IF

    @Column(name = "patente", length = 50)
    private String patente;

    @Column(name = "cnss", length = 50)
    private String cnss;

    @Column(name = "capital_social", precision = 15, scale = 2)
    private BigDecimal capitalSocial = BigDecimal.ZERO;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String adresse;

    @Column(name = "ville", length = 100)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(name = "telephone", length = 50)
    private String telephone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "site_web", length = 200)
    private String siteWeb;

    @Column(name = "activite_principale", length = 255)
    private String activitePrincipale;

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tva")
    private RegimeTva regimeTva = RegimeTva.ENCAISSEMENT;

    @Column(name = "periodicite_tva", length = 20)
    private String periodiciteTva = "MENSUELLE"; // MENSUELLE ou TRIMESTRIELLE

    @Column(name = "exercice_en_cours")
    private Integer exerciceEnCours = 2026;

    @Column(name = "date_cloture_exercice", length = 20)
    private String dateClotureExercice = "31/12";

    @Column(name = "responsable_dossier", length = 150)
    private String responsableDossier;

    @Column(name = "is_par_defaut")
    private Boolean isParDefaut = false;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Societe() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMereId() { return mereId != null ? mereId : tenantId; }
    public void setMereId(Long mereId) {
        this.mereId = mereId;
        if (this.tenantId == null) {
            this.tenantId = mereId;
        }
    }

    public Long getTenantId() { return tenantId != null ? tenantId : mereId; }
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        if (this.mereId == null) {
            this.mereId = tenantId;
        }
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getFormeJuridique() { return formeJuridique; }
    public void setFormeJuridique(String formeJuridique) { this.formeJuridique = formeJuridique; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getRc() { return rc; }
    public void setRc(String rc) { this.rc = rc; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getCnss() { return cnss; }
    public void setCnss(String cnss) { this.cnss = cnss; }

    public BigDecimal getCapitalSocial() { return capitalSocial; }
    public void setCapitalSocial(BigDecimal capitalSocial) { this.capitalSocial = capitalSocial; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getActivitePrincipale() { return activitePrincipale; }
    public void setActivitePrincipale(String activitePrincipale) { this.activitePrincipale = activitePrincipale; }

    public RegimeTva getRegimeTva() { return regimeTva; }
    public void setRegimeTva(RegimeTva regimeTva) { this.regimeTva = regimeTva; }

    public String getPeriodiciteTva() { return periodiciteTva; }
    public void setPeriodiciteTva(String periodiciteTva) { this.periodiciteTva = periodiciteTva; }

    public Integer getExerciceEnCours() { return exerciceEnCours; }
    public void setExerciceEnCours(Integer exerciceEnCours) { this.exerciceEnCours = exerciceEnCours; }

    public String getDateClotureExercice() { return dateClotureExercice; }
    public void setDateClotureExercice(String dateClotureExercice) { this.dateClotureExercice = dateClotureExercice; }

    public String getResponsableDossier() { return responsableDossier; }
    public void setResponsableDossier(String responsableDossier) { this.responsableDossier = responsableDossier; }

    public Boolean getIsParDefaut() { return isParDefaut; }
    public void setIsParDefaut(Boolean parDefaut) { isParDefaut = parDefaut; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
