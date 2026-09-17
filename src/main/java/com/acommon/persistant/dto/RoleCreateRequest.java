package com.acommon.persistant.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class RoleCreateRequest {
    @NotBlank
    private String nom;
    private String description;
    private List<String> habilitations;

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getHabilitations() { return habilitations; }
    public void setHabilitations(List<String> habilitations) { this.habilitations = habilitations; }
}
