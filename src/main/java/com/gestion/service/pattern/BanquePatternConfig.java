package com.gestion.service.pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BanquePatternConfig {

    private String code;
    private String nom;
    private List<String> identifiants;
    private String detectionRegex;
    private String anneeRegex;
    private String ligneTransactionRegex;
    private List<String> colonnes;
    private ReglesSens reglesSens;
    private List<String> lignesIgnorees;

    public static class ReglesSens {
        private String priorite;
        private List<String> motsClesCredit;
        private List<String> motsClesDebit;

        public String getPriorite() { return priorite; }
        public void setPriorite(String priorite) { this.priorite = priorite; }

        public List<String> getMotsClesCredit() { return motsClesCredit; }
        public void setMotsClesCredit(List<String> motsClesCredit) { this.motsClesCredit = motsClesCredit; }

        public List<String> getMotsClesDebit() { return motsClesDebit; }
        public void setMotsClesDebit(List<String> motsClesDebit) { this.motsClesDebit = motsClesDebit; }
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public List<String> getIdentifiants() { return identifiants; }
    public void setIdentifiants(List<String> identifiants) { this.identifiants = identifiants; }

    public String getDetectionRegex() { return detectionRegex; }
    public void setDetectionRegex(String detectionRegex) { this.detectionRegex = detectionRegex; }

    public String getAnneeRegex() { return anneeRegex; }
    public void setAnneeRegex(String anneeRegex) { this.anneeRegex = anneeRegex; }

    public String getLigneTransactionRegex() { return ligneTransactionRegex; }
    public void setLigneTransactionRegex(String ligneTransactionRegex) { this.ligneTransactionRegex = ligneTransactionRegex; }

    public List<String> getColonnes() { return colonnes; }
    public void setColonnes(List<String> colonnes) { this.colonnes = colonnes; }

    public ReglesSens getReglesSens() { return reglesSens; }
    public void setReglesSens(ReglesSens reglesSens) { this.reglesSens = reglesSens; }

    public List<String> getLignesIgnorees() { return lignesIgnorees; }
    public void setLignesIgnorees(List<String> lignesIgnorees) { this.lignesIgnorees = lignesIgnorees; }
}
