package com.gestion.persistent.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProduitCodeBarreDTO {

    private Long id;
    private String reference;
    private String designation;
    private String codeBarre;
    private String barcodeBase64;
    private BigDecimal prixVenteTtc;
    private String categorie;
    private Boolean actif;

    public ProduitCodeBarreDTO() {
    }

    public ProduitCodeBarreDTO(Long id, String reference, String designation, String codeBarre, String barcodeBase64, BigDecimal prixVenteTtc, String categorie, Boolean actif) {
        this.id = id;
        this.reference = reference;
        this.designation = designation;
        this.codeBarre = codeBarre;
        this.barcodeBase64 = barcodeBase64;
        this.prixVenteTtc = prixVenteTtc;
        this.categorie = categorie;
        this.actif = actif;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getCodeBarre() {
        return codeBarre;
    }

    public void setCodeBarre(String codeBarre) {
        this.codeBarre = codeBarre;
    }

    public String getBarcodeBase64() {
        return barcodeBase64;
    }

    public void setBarcodeBase64(String barcodeBase64) {
        this.barcodeBase64 = barcodeBase64;
    }

    public BigDecimal getPrixVenteTtc() {
        return prixVenteTtc;
    }

    public void setPrixVenteTtc(BigDecimal prixVenteTtc) {
        this.prixVenteTtc = prixVenteTtc;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }
}

