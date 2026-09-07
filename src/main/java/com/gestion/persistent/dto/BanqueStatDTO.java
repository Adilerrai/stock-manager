package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BanqueStatDTO {

    private Long banqueId;
    private String codeBanque;
    private String nomBanque;
    private String nomCourt;
    private String couleur;
    private String logo;
    private BigDecimal totalSolde = BigDecimal.ZERO;
    private int nombreComptes;
    private List<AgenceCompteStatDTO> comptesAgences = new ArrayList<>();

    public BanqueStatDTO() {}

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getCodeBanque() {
        return codeBanque;
    }

    public void setCodeBanque(String codeBanque) {
        this.codeBanque = codeBanque;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getNomCourt() {
        return nomCourt;
    }

    public void setNomCourt(String nomCourt) {
        this.nomCourt = nomCourt;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public BigDecimal getTotalSolde() {
        return totalSolde;
    }

    public void setTotalSolde(BigDecimal totalSolde) {
        this.totalSolde = totalSolde;
    }

    public int getNombreComptes() {
        return nombreComptes;
    }

    public void setNombreComptes(int nombreComptes) {
        this.nombreComptes = nombreComptes;
    }

    public List<AgenceCompteStatDTO> getComptesAgences() {
        return comptesAgences;
    }

    public void setComptesAgences(List<AgenceCompteStatDTO> comptesAgences) {
        this.comptesAgences = comptesAgences;
    }

    public static class AgenceCompteStatDTO {
        private Long compteId;
        private String codeCompte;
        private String nomCompte;
        private String agence;
        private String codeAgence;
        private String numeroCompteRib;
        private BigDecimal soldeActuel = BigDecimal.ZERO;
        private String devise;

        public AgenceCompteStatDTO() {}

        public AgenceCompteStatDTO(Long compteId, String codeCompte, String nomCompte, String agence,
                                   String codeAgence, String numeroCompteRib, BigDecimal soldeActuel, String devise) {
            this.compteId = compteId;
            this.codeCompte = codeCompte;
            this.nomCompte = nomCompte;
            this.agence = agence;
            this.codeAgence = codeAgence;
            this.numeroCompteRib = numeroCompteRib;
            this.soldeActuel = soldeActuel;
            this.devise = devise;
        }

        public Long getCompteId() {
            return compteId;
        }

        public void setCompteId(Long compteId) {
            this.compteId = compteId;
        }

        public String getCodeCompte() {
            return codeCompte;
        }

        public void setCodeCompte(String codeCompte) {
            this.codeCompte = codeCompte;
        }

        public String getNomCompte() {
            return nomCompte;
        }

        public void setNomCompte(String nomCompte) {
            this.nomCompte = nomCompte;
        }

        public String getAgence() {
            return agence;
        }

        public void setAgence(String agence) {
            this.agence = agence;
        }

        public String getCodeAgence() {
            return codeAgence;
        }

        public void setCodeAgence(String codeAgence) {
            this.codeAgence = codeAgence;
        }

        public String getNumeroCompteRib() {
            return numeroCompteRib;
        }

        public void setNumeroCompteRib(String numeroCompteRib) {
            this.numeroCompteRib = numeroCompteRib;
        }

        public BigDecimal getSoldeActuel() {
            return soldeActuel;
        }

        public void setSoldeActuel(BigDecimal soldeActuel) {
            this.soldeActuel = soldeActuel;
        }

        public String getDevise() {
            return devise;
        }

        public void setDevise(String devise) {
            this.devise = devise;
        }
    }
}
