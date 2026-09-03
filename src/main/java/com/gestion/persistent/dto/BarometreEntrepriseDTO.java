package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutSanteEntreprise;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BarometreEntrepriseDTO {
    private int scoreGlobal; // 0 à 100
    private StatutSanteEntreprise statutGlobal;
    private String statutLibelle;
    private String couleur;
    private Long pointDeVenteId;
    private LocalDateTime dateCalcul = LocalDateTime.now();

    // 5 Piliers
    private PilierSanteDTO tresorerie;
    private PilierSanteDTO recouvrement;
    private PilierSanteDTO rentabilite;
    private PilierSanteDTO commercial;
    private PilierSanteDTO stock;

    // Diagnostic & Recommandations stratégiques
    private List<String> pointsForts = new ArrayList<>();
    private List<String> pointsVigilance = new ArrayList<>();
    private List<String> recommandationsDirecteur = new ArrayList<>();

    public BarometreEntrepriseDTO() {}

    public int getScoreGlobal() { return scoreGlobal; }
    public void setScoreGlobal(int scoreGlobal) { this.scoreGlobal = scoreGlobal; }

    public StatutSanteEntreprise getStatutGlobal() { return statutGlobal; }
    public void setStatutGlobal(StatutSanteEntreprise statutGlobal) {
        this.statutGlobal = statutGlobal;
        if (statutGlobal != null) {
            this.statutLibelle = statutGlobal.getLibelle();
            this.couleur = statutGlobal.getCouleur();
        }
    }

    public String getStatutLibelle() { return statutLibelle; }
    public void setStatutLibelle(String statutLibelle) { this.statutLibelle = statutLibelle; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }

    public LocalDateTime getDateCalcul() { return dateCalcul; }
    public void setDateCalcul(LocalDateTime dateCalcul) { this.dateCalcul = dateCalcul; }

    public PilierSanteDTO getTresorerie() { return tresorerie; }
    public void setTresorerie(PilierSanteDTO tresorerie) { this.tresorerie = tresorerie; }

    public PilierSanteDTO getRecouvrement() { return recouvrement; }
    public void setRecouvrement(PilierSanteDTO recouvrement) { this.recouvrement = recouvrement; }

    public PilierSanteDTO getRentabilite() { return rentabilite; }
    public void setRentabilite(PilierSanteDTO rentabilite) { this.rentabilite = rentabilite; }

    public PilierSanteDTO getCommercial() { return commercial; }
    public void setCommercial(PilierSanteDTO commercial) { this.commercial = commercial; }

    public PilierSanteDTO getStock() { return stock; }
    public void setStock(PilierSanteDTO stock) { this.stock = stock; }

    public List<String> getPointsForts() { return pointsForts; }
    public void setPointsForts(List<String> pointsForts) { this.pointsForts = pointsForts; }

    public List<String> getPointsVigilance() { return pointsVigilance; }
    public void setPointsVigilance(List<String> pointsVigilance) { this.pointsVigilance = pointsVigilance; }

    public List<String> getRecommandationsDirecteur() { return recommandationsDirecteur; }
    public void setRecommandationsDirecteur(List<String> recommandationsDirecteur) { this.recommandationsDirecteur = recommandationsDirecteur; }
}
