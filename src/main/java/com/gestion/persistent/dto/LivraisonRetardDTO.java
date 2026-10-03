package com.gestion.persistent.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LivraisonRetardDTO {
    private Long id;
    private String type; // FOURNISSEUR ou CLIENT
    private String numeroCommande;
    private String numeroBl;
    private String nomTiers;
    private String telephoneTiers;
    private String emailTiers;
    private LocalDateTime dateCommande;
    private LocalDateTime dateLivraisonPrevue;
    private Long joursRetard;
    private String statut;
    private String statutCode;
    private BigDecimal montantTotal;
    private String articlesEnAttente;
    private Integer nombreArticlesTotal;
    private Integer nombreArticlesRestants;
    private Long pointDeVenteId;

    public LivraisonRetardDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public String getNumeroBl() { return numeroBl; }
    public void setNumeroBl(String numeroBl) { this.numeroBl = numeroBl; }

    public String getNomTiers() { return nomTiers; }
    public void setNomTiers(String nomTiers) { this.nomTiers = nomTiers; }

    public String getTelephoneTiers() { return telephoneTiers; }
    public void setTelephoneTiers(String telephoneTiers) { this.telephoneTiers = telephoneTiers; }

    public String getEmailTiers() { return emailTiers; }
    public void setEmailTiers(String emailTiers) { this.emailTiers = emailTiers; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public LocalDateTime getDateLivraisonPrevue() { return dateLivraisonPrevue; }
    public void setDateLivraisonPrevue(LocalDateTime dateLivraisonPrevue) { this.dateLivraisonPrevue = dateLivraisonPrevue; }

    public Long getJoursRetard() { return joursRetard; }
    public void setJoursRetard(Long joursRetard) { this.joursRetard = joursRetard; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getStatutCode() { return statutCode; }
    public void setStatutCode(String statutCode) { this.statutCode = statutCode; }

    public BigDecimal getMontantTotal() { return montantTotal; }
    public void setMontantTotal(BigDecimal montantTotal) { this.montantTotal = montantTotal; }

    public String getArticlesEnAttente() { return articlesEnAttente; }
    public void setArticlesEnAttente(String articlesEnAttente) { this.articlesEnAttente = articlesEnAttente; }

    public Integer getNombreArticlesTotal() { return nombreArticlesTotal; }
    public void setNombreArticlesTotal(Integer nombreArticlesTotal) { this.nombreArticlesTotal = nombreArticlesTotal; }

    public Integer getNombreArticlesRestants() { return nombreArticlesRestants; }
    public void setNombreArticlesRestants(Integer nombreArticlesRestants) { this.nombreArticlesRestants = nombreArticlesRestants; }

    public Long getPointDeVenteId() { return pointDeVenteId; }
    public void setPointDeVenteId(Long pointDeVenteId) { this.pointDeVenteId = pointDeVenteId; }
}
