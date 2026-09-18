package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT15DetailCpcDTO {

    private String titre = "TABLEAU 15 : DÉTAIL DES POSTES DU C.P.C.";
    private Integer annee;
    private List<RubriqueDetailCpcDTO> rubriques = new ArrayList<>();

    public LiasseTableauT15DetailCpcDTO() {}

    public static class RubriqueDetailCpcDTO {
        private String codePoste;
        private String libellePoste;
        private BigDecimal montantTotal = BigDecimal.ZERO;
        private List<SousCompteCpcDTO> sousComptes = new ArrayList<>();

        public RubriqueDetailCpcDTO() {}

        public RubriqueDetailCpcDTO(String codePoste, String libellePoste) {
            this.codePoste = codePoste;
            this.libellePoste = libellePoste;
        }

        public String getCodePoste() { return codePoste; }
        public void setCodePoste(String codePoste) { this.codePoste = codePoste; }

        public String getLibellePoste() { return libellePoste; }
        public void setLibellePoste(String libellePoste) { this.libellePoste = libellePoste; }

        public BigDecimal getMontantTotal() { return montantTotal; }
        public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

        public List<SousCompteCpcDTO> getSousComptes() { return sousComptes; }
        public void setSousComptes(List<SousCompteCpcDTO> sousComptes) { this.sousComptes = sousComptes; }
    }

    public static class SousCompteCpcDTO {
        private String numeroCompte;
        private String libelle;
        private BigDecimal montant = BigDecimal.ZERO;

        public SousCompteCpcDTO() {}

        public SousCompteCpcDTO(String numeroCompte, String libelle, BigDecimal montant) {
            this.numeroCompte = numeroCompte;
            this.libelle = libelle;
            this.montant = montant != null ? montant : BigDecimal.ZERO;
        }

        public String getNumeroCompte() { return numeroCompte; }
        public void setNumeroCompte(String numeroCompte) { this.numeroCompte = numeroCompte; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public BigDecimal getMontant() { return montant; }
        public void setMontant(BigDecimal montant) { this.montant = montant; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<RubriqueDetailCpcDTO> getRubriques() { return rubriques; }
    public void setRubriques(List<RubriqueDetailCpcDTO> rubriques) { this.rubriques = rubriques; }
}
