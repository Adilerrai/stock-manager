package com.gestion.persistent.dto;

import java.math.BigDecimal;

public class MatriceAbcRotationDTO {

    public static class CelluleMatriceDTO {
        private String code; // ex: "A_RAPIDE", "A_LENTE"
        private String libelle;
        private long nombreReferences = 0;
        private BigDecimal valeurTotaleDH = BigDecimal.ZERO;
        private BigDecimal pourcentageValeurTotale = BigDecimal.ZERO;
        private String niveauAlerte; // "VERT", "ORANGE", "ROUGE"

        public CelluleMatriceDTO() {}

        public CelluleMatriceDTO(String code, String libelle, String niveauAlerte) {
            this.code = code;
            this.libelle = libelle;
            this.niveauAlerte = niveauAlerte;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public String getLibelle() { return libelle; }
        public void setLibelle(String libelle) { this.libelle = libelle; }

        public long getNombreReferences() { return nombreReferences; }
        public void setNombreReferences(long nombreReferences) { this.nombreReferences = nombreReferences; }

        public BigDecimal getValeurTotaleDH() { return valeurTotaleDH; }
        public void setValeurTotaleDH(BigDecimal valeurTotaleDH) { this.valeurTotaleDH = valeurTotaleDH; }

        public BigDecimal getPourcentageValeurTotale() { return pourcentageValeurTotale; }
        public void setPourcentageValeurTotale(BigDecimal pourcentageValeurTotale) { this.pourcentageValeurTotale = pourcentageValeurTotale; }

        public String getNiveauAlerte() { return niveauAlerte; }
        public void setNiveauAlerte(String niveauAlerte) { this.niveauAlerte = niveauAlerte; }

        public void ajouterProduit(BigDecimal valeur) {
            this.nombreReferences++;
            if (valeur != null) {
                this.valeurTotaleDH = this.valeurTotaleDH.add(valeur);
            }
        }
    }

    private CelluleMatriceDTO aRapide = new CelluleMatriceDTO("A_RAPIDE", "A / Rapide", "VERT");
    private CelluleMatriceDTO aMoyenne = new CelluleMatriceDTO("A_MOYENNE", "A / Normale", "VERT");
    private CelluleMatriceDTO aLente = new CelluleMatriceDTO("A_LENTE", "A / Lente (Danger Cash)", "ROUGE");

    private CelluleMatriceDTO bRapide = new CelluleMatriceDTO("B_RAPIDE", "B / Rapide", "VERT");
    private CelluleMatriceDTO bMoyenne = new CelluleMatriceDTO("B_MOYENNE", "B / Normale", "VERT");
    private CelluleMatriceDTO bLente = new CelluleMatriceDTO("B_LENTE", "B / Lente", "ORANGE");

    private CelluleMatriceDTO cRapide = new CelluleMatriceDTO("C_RAPIDE", "C / Rapide", "VERT");
    private CelluleMatriceDTO cMoyenne = new CelluleMatriceDTO("C_MOYENNE", "C / Normale", "VERT");
    private CelluleMatriceDTO cLente = new CelluleMatriceDTO("C_LENTE", "C / Lente (Dormant)", "ORANGE");

    public MatriceAbcRotationDTO() {}

    public CelluleMatriceDTO getaRapide() { return aRapide; }
    public void setaRapide(CelluleMatriceDTO aRapide) { this.aRapide = aRapide; }

    public CelluleMatriceDTO getaMoyenne() { return aMoyenne; }
    public void setaMoyenne(CelluleMatriceDTO aMoyenne) { this.aMoyenne = aMoyenne; }

    public CelluleMatriceDTO getaLente() { return aLente; }
    public void setaLente(CelluleMatriceDTO aLente) { this.aLente = aLente; }

    public CelluleMatriceDTO getbRapide() { return bRapide; }
    public void setbRapide(CelluleMatriceDTO bRapide) { this.bRapide = bRapide; }

    public CelluleMatriceDTO getbMoyenne() { return bMoyenne; }
    public void setbMoyenne(CelluleMatriceDTO bMoyenne) { this.bMoyenne = bMoyenne; }

    public CelluleMatriceDTO getbLente() { return bLente; }
    public void setbLente(CelluleMatriceDTO bLente) { this.bLente = bLente; }

    public CelluleMatriceDTO getcRapide() { return cRapide; }
    public void setcRapide(CelluleMatriceDTO cRapide) { this.cRapide = cRapide; }

    public CelluleMatriceDTO getcMoyenne() { return cMoyenne; }
    public void setcMoyenne(CelluleMatriceDTO cMoyenne) { this.cMoyenne = cMoyenne; }

    public CelluleMatriceDTO getcLente() { return cLente; }
    public void setcLente(CelluleMatriceDTO cLente) { this.cLente = cLente; }
}
