package com.gestion.persistent.dto;

import java.util.ArrayList;
import java.util.List;

public class LiasseTableauT19DerogationsDTO {

    private String titre = "TABLEAUX 19 & 20 : DÉROGATIONS AUX PRINCIPES COMPTABLES & CHANGEMENTS DE MÉTHODES";
    private Integer annee;
    private List<LigneDerogationDTO> derogations = new ArrayList<>();
    private List<LigneChangementMethodeDTO> changementsMethodes = new ArrayList<>();

    public LiasseTableauT19DerogationsDTO() {}

    public static class LigneDerogationDTO {
        private String principe;
        private String derogationPratiquee;
        private String justifications;
        private String influenceSurPatrimoineEtResultats;

        public LigneDerogationDTO() {}

        public LigneDerogationDTO(String principe, String derogationPratiquee, String justifications, String influence) {
            this.principe = principe;
            this.derogationPratiquee = derogationPratiquee;
            this.justifications = justifications;
            this.influenceSurPatrimoineEtResultats = influence;
        }

        public String getPrincipe() { return principe; }
        public void setPrincipe(String principe) { this.principe = principe; }

        public String getDerogationPratiquee() { return derogationPratiquee; }
        public void setDerogationPratiquee(String derogationPratiquee) { this.derogationPratiquee = derogationPratiquee; }

        public String getJustifications() { return justifications; }
        public void setJustifications(String justifications) { this.justifications = justifications; }

        public String getInfluenceSurPatrimoineEtResultats() { return influenceSurPatrimoineEtResultats; }
        public void setInfluenceSurPatrimoineEtResultats(String influenceSurPatrimoineEtResultats) { this.influenceSurPatrimoineEtResultats = influenceSurPatrimoineEtResultats; }
    }

    public static class LigneChangementMethodeDTO {
        private String natureChangement;
        private String justifications;
        private String influenceSurResultatsEtBilan;

        public LigneChangementMethodeDTO() {}

        public LigneChangementMethodeDTO(String natureChangement, String justifications, String influence) {
            this.natureChangement = natureChangement;
            this.justifications = justifications;
            this.influenceSurResultatsEtBilan = influence;
        }

        public String getNatureChangement() { return natureChangement; }
        public void setNatureChangement(String natureChangement) { this.natureChangement = natureChangement; }

        public String getJustifications() { return justifications; }
        public void setJustifications(String justifications) { this.justifications = justifications; }

        public String getInfluenceSurResultatsEtBilan() { return influenceSurResultatsEtBilan; }
        public void setInfluenceSurResultatsEtBilan(String influenceSurResultatsEtBilan) { this.influenceSurResultatsEtBilan = influenceSurResultatsEtBilan; }
    }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public Integer getAnnee() { return annee; }
    public void setAnnee(Integer annee) { this.annee = annee; }

    public List<LigneDerogationDTO> getDerogations() { return derogations; }
    public void setDerogations(List<LigneDerogationDTO> derogations) { this.derogations = derogations; }

    public List<LigneChangementMethodeDTO> getChangementsMethodes() { return changementsMethodes; }
    public void setChangementsMethodes(List<LigneChangementMethodeDTO> changementsMethodes) { this.changementsMethodes = changementsMethodes; }
}
