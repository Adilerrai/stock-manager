package com.gestion.persistent.dto;

public class PilierSanteDTO {
    private String nom;
    private int score; // 0 à 100
    private int poids; // Pondération en %
    private String valeurCle;
    private String commentaire;
    private String statut;
    private String couleur;

    public PilierSanteDTO() {}

    public PilierSanteDTO(String nom, int score, int poids, String valeurCle, String commentaire, String statut, String couleur) {
        this.nom = nom;
        this.score = score;
        this.poids = poids;
        this.valeurCle = valeurCle;
        this.commentaire = commentaire;
        this.statut = statut;
        this.couleur = couleur;
    }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getPoids() { return poids; }
    public void setPoids(int poids) { this.poids = poids; }

    public String getValeurCle() { return valeurCle; }
    public void setValeurCle(String valeurCle) { this.valeurCle = valeurCle; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }
}
