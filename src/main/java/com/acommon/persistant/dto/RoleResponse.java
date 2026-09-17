package com.acommon.persistant.dto;

import java.util.List;

public class RoleResponse {
    private Long id;
    private String nom;
    private List<String> habilitations;
    private long nombreUtilisateurs;

    public RoleResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public List<String> getHabilitations() { return habilitations; }
    public void setHabilitations(List<String> habilitations) { this.habilitations = habilitations; }

    public long getNombreUtilisateurs() { return nombreUtilisateurs; }
    public void setNombreUtilisateurs(long nombreUtilisateurs) { this.nombreUtilisateurs = nombreUtilisateurs; }
}
