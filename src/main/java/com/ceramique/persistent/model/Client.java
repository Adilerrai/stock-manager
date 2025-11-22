package com.ceramique.persistent.model;

import com.acommon.persistant.model.PointDeVente;
import com.ceramique.persistent.enums.CategorieClient;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    private String prenom;

    @Column(name = "nom_complet")
    private String nomComplet;

    private String telephone;

    private String email;

    private String adresse;

    private String ville;

    private String codePostal;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie")
    private CategorieClient categorie = CategorieClient.PARTICULIER;

    @Column(name = "numero_registre_commerce")
    private String numeroRegistreCommerce; // Pour les professionnels

    @Column(name = "numero_identification_fiscale")
    private String numeroIdentificationFiscale; // NIF

    @Column(name = "credit_autorise")
    private BigDecimal creditAutorise = BigDecimal.ZERO;

    @Column(name = "credit_utilise")
    private BigDecimal creditUtilise = BigDecimal.ZERO;

    @Column(name = "points_fidelite")
    private Integer pointsFidelite = 0;

    private Boolean actif = true;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_derniere_visite")
    private LocalDateTime dateDerniereVisite;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_de_vente_id", nullable = false)
    private PointDeVente pointDeVente;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Vente> ventes = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Facture> factures = new ArrayList<>();


    // Constructeurs
    public Client() {
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public CategorieClient getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieClient categorie) {
        this.categorie = categorie;
    }

    public String getNumeroRegistreCommerce() {
        return numeroRegistreCommerce;
    }

    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) {
        this.numeroRegistreCommerce = numeroRegistreCommerce;
    }

    public String getNumeroIdentificationFiscale() {
        return numeroIdentificationFiscale;
    }

    public void setNumeroIdentificationFiscale(String numeroIdentificationFiscale) {
        this.numeroIdentificationFiscale = numeroIdentificationFiscale;
    }

    public BigDecimal getCreditAutorise() {
        return creditAutorise;
    }

    public void setCreditAutorise(BigDecimal creditAutorise) {
        this.creditAutorise = creditAutorise;
    }

    public BigDecimal getCreditUtilise() {
        return creditUtilise;
    }

    public void setCreditUtilise(BigDecimal creditUtilise) {
        this.creditUtilise = creditUtilise;
    }

    public Integer getPointsFidelite() {
        return pointsFidelite;
    }

    public void setPointsFidelite(Integer pointsFidelite) {
        this.pointsFidelite = pointsFidelite;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateDerniereVisite() {
        return dateDerniereVisite;
    }

    public void setDateDerniereVisite(LocalDateTime dateDerniereVisite) {
        this.dateDerniereVisite = dateDerniereVisite;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public PointDeVente getPointDeVente() {
        return pointDeVente;
    }

    public void setPointDeVente(PointDeVente pointDeVente) {
        this.pointDeVente = pointDeVente;
    }

    public List<Vente> getVentes() {
        return ventes;
    }

    public void setVentes(List<Vente> ventes) {
        this.ventes = ventes;
    }

    public List<Facture> getFactures() {
        return factures;
    }

    public void setFactures(List<Facture> factures) {
        this.factures = factures;
    }

    // Méthodes utilitaires
    public BigDecimal getCreditDisponible() {
        return creditAutorise.subtract(creditUtilise);
    }

    public boolean peutAcheterACredit(BigDecimal montant) {
        return getCreditDisponible().compareTo(montant) >= 0;
    }
}
