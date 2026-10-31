package com.gestion.persistent.dto;

import com.gestion.persistent.enums.StatutCommande;
import com.gestion.persistent.enums.TypeAchat;
import com.gestion.persistent.enums.TypeFournisseur;
import java.time.LocalDateTime;

public class CommandeSearchCriteria {
    private String numeroCommande;
    private Long fournisseurId;
    private StatutCommande statut;
    private LocalDateTime dateDebutCommande;
    private LocalDateTime dateFinCommande;
    private LocalDateTime dateDebutLivraison;
    private LocalDateTime dateFinLivraison;
    private Long societeId;
    private Boolean inclureAnnulees;
    private TypeAchat typeAchat;
    private TypeFournisseur typeFournisseur;
    private Boolean isInternational;
    private String statutWorkflowImport;
    private String numeroPi;
    private String numeroEngagementPortnet;

    public Boolean getInclureAnnulees() { return inclureAnnulees; }
    public void setInclureAnnulees(Boolean inclureAnnulees) { this.inclureAnnulees = inclureAnnulees; }

    // Getters et Setters
    public String getNumeroCommande() { return numeroCommande; }
    public void setNumeroCommande(String numeroCommande) { this.numeroCommande = numeroCommande; }

    public Long getFournisseurId() { return fournisseurId; }
    public void setFournisseurId(Long fournisseurId) { this.fournisseurId = fournisseurId; }

    public StatutCommande getStatut() { return statut; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }

    public LocalDateTime getDateDebutCommande() { return dateDebutCommande; }
    public void setDateDebutCommande(LocalDateTime dateDebutCommande) { this.dateDebutCommande = dateDebutCommande; }

    public LocalDateTime getDateFinCommande() { return dateFinCommande; }
    public void setDateFinCommande(LocalDateTime dateFinCommande) { this.dateFinCommande = dateFinCommande; }

    public LocalDateTime getDateDebutLivraison() { return dateDebutLivraison; }
    public void setDateDebutLivraison(LocalDateTime dateDebutLivraison) { this.dateDebutLivraison = dateDebutLivraison; }

    public LocalDateTime getDateFinLivraison() { return dateFinLivraison; }
    public void setDateFinLivraison(LocalDateTime dateFinLivraison) { this.dateFinLivraison = dateFinLivraison; }

    public Long getSocieteId() { return societeId; }
    public void setSocieteId(Long societeId) { this.societeId = societeId; }

    public TypeAchat getTypeAchat() {
        if (typeAchat != null) {
            return typeAchat;
        }
        if (typeFournisseur != null) {
            try {
                return TypeAchat.valueOf(typeFournisseur.name());
            } catch (Exception ignored) {}
        }
        if (Boolean.TRUE.equals(isInternational)) {
            return TypeAchat.INTERNATIONAL;
        } else if (Boolean.FALSE.equals(isInternational)) {
            return TypeAchat.NATIONAL;
        }
        return null;
    }

    public void setTypeAchat(TypeAchat typeAchat) {
        this.typeAchat = typeAchat;
        if (typeAchat != null) {
            try {
                this.typeFournisseur = TypeFournisseur.valueOf(typeAchat.name());
            } catch (Exception ignored) {}
            this.isInternational = (typeAchat == TypeAchat.INTERNATIONAL);
        }
    }

    public TypeFournisseur getTypeFournisseur() {
        if (typeFournisseur != null) {
            return typeFournisseur;
        }
        if (typeAchat != null) {
            try {
                return TypeFournisseur.valueOf(typeAchat.name());
            } catch (Exception ignored) {}
        }
        if (Boolean.TRUE.equals(isInternational)) {
            return TypeFournisseur.INTERNATIONAL;
        } else if (Boolean.FALSE.equals(isInternational)) {
            return TypeFournisseur.NATIONAL;
        }
        return null;
    }

    public void setTypeFournisseur(TypeFournisseur typeFournisseur) {
        this.typeFournisseur = typeFournisseur;
        if (typeFournisseur != null) {
            try {
                this.typeAchat = TypeAchat.valueOf(typeFournisseur.name());
            } catch (Exception ignored) {}
            this.isInternational = (typeFournisseur == TypeFournisseur.INTERNATIONAL);
        }
    }

    public Boolean getIsInternational() {
        if (isInternational != null) {
            return isInternational;
        }
        if (typeAchat != null) {
            return typeAchat == TypeAchat.INTERNATIONAL;
        }
        if (typeFournisseur != null) {
            return typeFournisseur == TypeFournisseur.INTERNATIONAL;
        }
        return null;
    }

    public void setIsInternational(Boolean isInternational) {
        this.isInternational = isInternational;
        if (isInternational != null) {
            this.typeAchat = isInternational ? TypeAchat.INTERNATIONAL : TypeAchat.NATIONAL;
            this.typeFournisseur = isInternational ? TypeFournisseur.INTERNATIONAL : TypeFournisseur.NATIONAL;
        }
    }

    public void setType(String type) {
        if (type != null && !type.trim().isEmpty()) {
            String val = type.trim().toUpperCase();
            try {
                this.typeAchat = TypeAchat.valueOf(val);
            } catch (Exception ignored) {}
            try {
                this.typeFournisseur = TypeFournisseur.valueOf(val);
            } catch (Exception ignored) {}
            if ("INTERNATIONAL".equals(val)) {
                this.isInternational = true;
            } else if ("NATIONAL".equals(val)) {
                this.isInternational = false;
            }
        }
    }

    public String getStatutWorkflowImport() {
        return statutWorkflowImport;
    }

    public void setStatutWorkflowImport(String statutWorkflowImport) {
        this.statutWorkflowImport = statutWorkflowImport;
    }

    public String getNumeroPi() {
        return numeroPi;
    }

    public void setNumeroPi(String numeroPi) {
        this.numeroPi = numeroPi;
    }

    public String getNumeroEngagementPortnet() {
        return numeroEngagementPortnet;
    }

    public void setNumeroEngagementPortnet(String numeroEngagementPortnet) {
        this.numeroEngagementPortnet = numeroEngagementPortnet;
    }
}
