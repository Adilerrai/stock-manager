package com.acommon.persistant.dto;

public class PointDeVenteResponse {

    private Long id;
    private String nomPointDeVente;
    private Long tenantId;
    private long nombreUtilisateurs;

    // Getters et Setters
    public String getNomPointDeVente() {
        return nomPointDeVente;
    }

    public void setNomPointDeVente(String nomPointDeVente) {
        this.nomPointDeVente = nomPointDeVente;
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getTenantId() {
        return tenantId;
    }
    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
    public long getNombreUtilisateurs() {
        return nombreUtilisateurs;
    }
    public void setNombreUtilisateurs(long nombreUtilisateurs) {
        this.nombreUtilisateurs = nombreUtilisateurs;
    }


}