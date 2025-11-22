package com.acommon.persistant.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PointDeVenteCreationRequest {

    @NotBlank(message = "Le nom du point de vente est obligatoire")
    @Size(min = 3, max = 100, message = "Le nom du point de vente doit contenir entre 3 et 100 caractères")
    private String nomPointDeVente;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    // Getters et Setters
    public String getNomPointDeVente() {
        return nomPointDeVente;
    }

    public void setNomPointDeVente(String nomPointDeVente) {
        this.nomPointDeVente = nomPointDeVente;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
