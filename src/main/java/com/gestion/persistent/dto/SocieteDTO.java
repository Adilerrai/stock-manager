package com.gestion.persistent.dto;

import com.gestion.persistent.enums.RegimeTva;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SocieteDTO {
    private Long id;
    private Long mereId;
    private Long tenantId;
    private String code;
    private String raisonSociale;
    private String formeJuridique;
    private String ice;
    private String rc;
    private String identifiantFiscal;
    private String patente;
    private String cnss;
    private BigDecimal capitalSocial;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String email;
    private String siteWeb;
    private String activitePrincipale;
    private RegimeTva regimeTva;
    private String periodiciteTva;
    private Integer exerciceEnCours;
    private String dateClotureExercice;
    private String responsableDossier;
    private Boolean isParDefaut;
    private Boolean actif;
    private LocalDateTime dateCreation;

    public SocieteDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getMereId() { return mereId != null ? mereId : tenantId; }
    public void setMereId(Long mereId) {
        this.mereId = mereId;
        if (this.tenantId == null) this.tenantId = mereId;
    }

    public Long getTenantId() { return tenantId != null ? tenantId : mereId; }
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        if (this.mereId == null) this.mereId = tenantId;
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
