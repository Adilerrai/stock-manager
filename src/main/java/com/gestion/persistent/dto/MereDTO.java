package com.gestion.persistent.dto;

import java.time.LocalDateTime;

public class MereDTO {

    private Long id;
    private String nom;
    private String ice;
    private String rc;
    private String identifiantFiscal;
    private String patente;
    private String formeJuridique;
    private String adresse;
    private String ville;
    private String codePostal;
    private String telephone;
    private String email;
    private String siteWeb;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private Long nombreSocietes;

    public MereDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getIce() { return ice; }
    public void setIce(String ice) { this.ice = ice; }

    public String getRc() { return rc; }
    public void setRc(String rc) { this.rc = rc; }

    public String getIdentifiantFiscal() { return identifiantFiscal; }
    public void setIdentifiantFiscal(String identifiantFiscal) { this.identifiantFiscal = identifiantFiscal; }

    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }

    public String getFormeJuridique() { return formeJuridique; }
    public void setFormeJuridique(String formeJuridique) { this.formeJuridique = formeJuridique; }

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

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public Long getNombreSocietes() { return nombreSocietes; }
    public void setNombreSocietes(Long nombreSocietes) { this.nombreSocietes = nombreSocietes; }
}
